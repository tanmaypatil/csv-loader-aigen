package com.csvloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports database table schemas to CSV files containing only column headers.
 * This class queries the database schema and generates CSV files with column names
 * as the first row, ready to be populated with data.
 */
public class CSVExporter {
    private static final Logger logger = LoggerFactory.getLogger(CSVExporter.class);

    private final DatabaseConnection databaseConnection;
    private final String delimiter;

    /**
     * Creates a new CSVExporter with the specified database connection and delimiter.
     *
     * @param databaseConnection the database connection to use for schema queries
     * @param delimiter the delimiter to use in the CSV files (e.g., ",", ";", "|")
     */
    public CSVExporter(DatabaseConnection databaseConnection, String delimiter) {
        this.databaseConnection = databaseConnection;
        this.delimiter = delimiter;
    }

    /**
     * Exports table headers for the specified list of tables to CSV files.
     * Creates one CSV file per table with column names as the first row.
     *
     * @param tableNames list of table names to export
     * @param outputDir the directory where CSV files will be created
     * @return the number of CSV files successfully created
     * @throws IOException if there's an error creating or writing to the CSV files
     */
    public int exportTableHeaders(List<String> tableNames, String outputDir) throws IOException {
        if (tableNames == null || tableNames.isEmpty()) {
            logger.warn("No tables specified for export");
            return 0;
        }

        // Create output directory if it doesn't exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Created output directory: {}", outputDir);
            } else {
                throw new IOException("Failed to create output directory: " + outputDir);
            }
        }

        int exportedCount = 0;
        for (String tableName : tableNames) {
            try {
                exportTableHeader(tableName.trim(), outputDir);
                exportedCount++;
                logger.info("Exported headers for table: {}", tableName);
            } catch (SQLException e) {
                logger.error("Failed to export headers for table '{}': {}", tableName, e.getMessage());
                // Continue with next table instead of failing completely
            }
        }

        return exportedCount;
    }

    /**
     * Exports headers for a single table to a CSV file.
     *
     * @param tableName the name of the table to export
     * @param outputDir the directory where the CSV file will be created
     * @throws SQLException if there's an error querying the database schema
     * @throws IOException if there's an error creating or writing to the CSV file
     */
    private void exportTableHeader(String tableName, String outputDir) throws SQLException, IOException {
        List<String> columnNames = getColumnNames(tableName);

        if (columnNames.isEmpty()) {
            logger.warn("No columns found for table '{}'. Table may not exist.", tableName);
            throw new SQLException("Table '" + tableName + "' not found or has no columns");
        }

        // Create CSV file path
        String fileName = tableName + ".csv";
        File csvFile = new File(outputDir, fileName);

        // Write column names to CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            String headerLine = String.join(delimiter, columnNames);
            writer.write(headerLine);
            writer.newLine();
            logger.debug("Wrote {} columns to {}: {}", columnNames.size(), fileName, headerLine);
        }
    }

    /**
     * Retrieves the column names for a specified table from the database schema.
     * Uses PostgreSQL's information_schema.columns to query column metadata.
     *
     * @param tableName the name of the table
     * @return list of column names ordered by their position in the table
     * @throws SQLException if there's an error querying the database
     */
    private List<String> getColumnNames(String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();

        String query = "SELECT column_name " +
                      "FROM information_schema.columns " +
                      "WHERE table_name = ? " +
                      "ORDER BY ordinal_position";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, tableName.toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    columnNames.add(columnName);
                }
            }
        }

        logger.debug("Retrieved {} columns for table '{}'", columnNames.size(), tableName);
        return columnNames;
    }

    /**
     * Gets the output directory path for exported CSV files.
     * Creates a subdirectory within the resources folder.
     *
     * @return the absolute path to the export output directory
     */
    public static String getDefaultOutputDir() {
        // Get the resources directory path
        String resourcesPath = CSVExporter.class.getClassLoader().getResource("").getPath();
        return resourcesPath + "exported-csv";
    }
}
