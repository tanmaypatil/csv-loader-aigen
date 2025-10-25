package com.csvloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple CSV importer that reads CSV files and inserts data into database tables.
 * Table name is derived from CSV filename (e.g., employee.csv -> employee table).
 */
public class CSVImporter {

    private static final Logger logger = LoggerFactory.getLogger(CSVImporter.class);

    private final DatabaseConnection dbConnection;
    private final ApplicationConfig appConfig;
    private final DateFormatConfig dateConfig;
    private final String delimiter;

    /**
     * Creates a CSVImporter with database connection and application configuration.
     * @param dbConnection database connection utility
     * @param appConfig application configuration
     * @throws IOException if configuration cannot be loaded
     */
    public CSVImporter(DatabaseConnection dbConnection, ApplicationConfig appConfig) throws IOException {
        logger.debug("Initializing CSVImporter");
        this.dbConnection = dbConnection;
        this.appConfig = appConfig;

        // Load date format from configuration
        String datePattern = appConfig.getDateFormat();
        this.dateConfig = new DateFormatConfig(datePattern);
        logger.info("Date format configured as: {}", datePattern);

        // Load CSV delimiter from configuration
        this.delimiter = appConfig.getCsvDelimiter();
        logger.debug("CSV delimiter configured as: '{}'", delimiter);
    }

    /**
     * Creates a CSVImporter with database connection (uses default ApplicationConfig).
     * @param dbConnection database connection utility
     * @throws IOException if configuration cannot be loaded
     * @deprecated Use {@link #CSVImporter(DatabaseConnection, ApplicationConfig)} instead
     */
    @Deprecated
    public CSVImporter(DatabaseConnection dbConnection) throws IOException {
        this(dbConnection, new ApplicationConfig());
    }

    /**
     * Imports a CSV file into the database.
     * @param csvFileName name of CSV file (e.g., "employee.csv")
     * @return number of rows imported
     * @throws IOException if file cannot be read
     * @throws SQLException if database operation fails
     */
    public int importCSV(String csvFileName) throws IOException, SQLException {
        // Derive table name from filename (remove .csv extension)
        String tableName = csvFileName.replace(".csv", "").toLowerCase();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFileName)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found: " + csvFileName);
            }

            return importCSV(inputStream, tableName);
        }
    }

    /**
     * Imports CSV data from an InputStream into specified table.
     * @param inputStream CSV data stream
     * @param tableName target table name
     * @return number of rows imported
     * @throws IOException if stream cannot be read
     * @throws SQLException if database operation fails
     */
    public int importCSV(InputStream inputStream, String tableName) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             Connection conn = dbConnection.getConnection()) {

            // Read header row (column names)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            String[] columnNames = parseLine(headerLine);

            // Get column types from database table
            String[] columnTypes = getColumnTypes(conn, tableName, columnNames);

            // Build INSERT query
            String insertQuery = buildInsertQuery(tableName, columnNames);

            int rowCount = 0;
            String line;

            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                // Read and insert data rows
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue; // Skip empty lines
                    }

                    String[] values = parseLine(line);

                    if (values.length != columnNames.length) {
                        throw new IOException("Row has " + values.length +
                            " columns, expected " + columnNames.length);
                    }

                    // Set parameters based on column types
                    for (int i = 0; i < values.length; i++) {
                        setParameter(pstmt, i + 1, values[i], columnTypes[i]);
                    }

                    pstmt.executeUpdate();
                    rowCount++;
                }
            }

            return rowCount;
        }
    }

    /**
     * Parses a CSV line into array of values.
     * @param line CSV line
     * @return array of trimmed values
     */
    private String[] parseLine(String line) {
        String[] parts = line.split(delimiter, -1); // -1 keeps trailing empty strings
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    /**
     * Builds parameterized INSERT query.
     * @param tableName table name
     * @param columnNames column names
     * @return SQL INSERT query
     */
    private String buildInsertQuery(String tableName, String[] columnNames) {
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(tableName).append(" (");

        // Add column names
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) query.append(", ");
            query.append(columnNames[i]);
        }

        query.append(") VALUES (");

        // Add parameter placeholders
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) query.append(", ");
            query.append("?");
        }

        query.append(")");
        return query.toString();
    }

    /**
     * Gets column data types from database table.
     * @param conn database connection
     * @param tableName table name
     * @param columnNames column names
     * @return array of column types
     * @throws SQLException if table metadata cannot be retrieved
     */
    private String[] getColumnTypes(Connection conn, String tableName, String[] columnNames)
            throws SQLException {
        String[] types = new String[columnNames.length];

        // Query to get column types - works for PostgreSQL
        String query = "SELECT column_name, data_type FROM information_schema.columns " +
                      "WHERE table_name = ? AND column_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < columnNames.length; i++) {
                pstmt.setString(1, tableName.toLowerCase());
                pstmt.setString(2, columnNames[i].toLowerCase());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        types[i] = rs.getString("data_type").toLowerCase();
                    } else {
                        throw new SQLException("Column not found: " + columnNames[i]);
                    }
                }
            }
        }

        return types;
    }

    /**
     * Sets PreparedStatement parameter based on column type.
     * @param pstmt PreparedStatement
     * @param paramIndex parameter index (1-based)
     * @param value string value from CSV
     * @param columnType database column type
     * @throws SQLException if parameter cannot be set
     */
    private void setParameter(PreparedStatement pstmt, int paramIndex, String value, String columnType)
            throws SQLException {

        // Handle NULL values
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("null")) {
            pstmt.setNull(paramIndex, java.sql.Types.NULL);
            return;
        }

        try {
            // Handle different data types
            if (columnType.contains("int") || columnType.contains("serial")) {
                pstmt.setInt(paramIndex, Integer.parseInt(value));
            }
            else if (columnType.contains("bigint")) {
                pstmt.setLong(paramIndex, Long.parseLong(value));
            }
            else if (columnType.contains("decimal") || columnType.contains("numeric")) {
                pstmt.setBigDecimal(paramIndex, new java.math.BigDecimal(value));
            }
            else if (columnType.contains("real") || columnType.contains("float")) {
                pstmt.setFloat(paramIndex, Float.parseFloat(value));
            }
            else if (columnType.contains("double")) {
                pstmt.setDouble(paramIndex, Double.parseDouble(value));
            }
            else if (columnType.contains("boolean")) {
                pstmt.setBoolean(paramIndex, Boolean.parseBoolean(value));
            }
            else if (columnType.contains("date")) {
                // Parse date using flexible parser (handles both date-only and full datetime)
                // For DATE columns, only the date portion is stored
                try {
                    java.time.ZonedDateTime zonedDateTime = dateConfig.parseFlexible(value);
                    java.sql.Date sqlDate = dateConfig.toSqlDate(zonedDateTime);
                    pstmt.setDate(paramIndex, sqlDate);
                } catch (java.time.format.DateTimeParseException e) {
                    throw new SQLException("Invalid date format: " + value +
                        ". Expected format: " + dateConfig.getDatePattern() +
                        " or yyyy-MM-dd (date-only, defaults to 00:00:00 UTC)", e);
                }
            }
            else if (columnType.contains("timestamp")) {
                // Parse timestamp using flexible parser
                // Supports full datetime format or date-only (which adds 00:00:00 UTC)
                try {
                    java.time.ZonedDateTime zonedDateTime = dateConfig.parseFlexible(value);
                    java.sql.Timestamp sqlTimestamp = dateConfig.toSqlTimestamp(zonedDateTime);
                    pstmt.setTimestamp(paramIndex, sqlTimestamp);
                } catch (java.time.format.DateTimeParseException e) {
                    throw new SQLException("Invalid timestamp format: " + value +
                        ". Expected format: " + dateConfig.getDatePattern() +
                        " or yyyy-MM-dd (date-only, defaults to 00:00:00 UTC)", e);
                }
            }
            else {
                // Default to string for varchar, text, etc.
                pstmt.setString(paramIndex, value);
            }
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid number format for column type " + columnType + ": " + value, e);
        }
    }

    /**
     * Gets the configured date pattern.
     * @return date pattern string
     */
    public String getDatePattern() {
        return dateConfig.getDatePattern();
    }
}
