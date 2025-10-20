package com.csvloader;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Main application class for CSV Database Importer.
 * Usage: java -jar csv-database-importer.jar <csv-filename>
 *
 * Example: java -jar csv-database-importer.jar employee.csv
 */
public class CSVImporterApp {

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String csvFileName = args[0];

        try {
            // Initialize database connection
            System.out.println("Connecting to database...");
            DatabaseConnection dbConnection = new DatabaseConnection();

            // Create CSV importer
            CSVImporter importer = new CSVImporter(dbConnection);

            System.out.println("Date format configured as: " + importer.getDatePattern());
            System.out.println("Importing CSV file: " + csvFileName);

            // Import CSV
            int rowCount = importer.importCSV(csvFileName);

            // Success message
            System.out.println("Successfully imported " + rowCount + " rows");
            System.exit(0);

        } catch (IOException e) {
            System.err.println("ERROR: Failed to read CSV file");
            System.err.println("Details: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);

        } catch (SQLException e) {
            System.err.println("ERROR: Database operation failed");
            System.err.println("Details: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);

        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error occurred");
            System.err.println("Details: " + e.getMessage());
            e.printStackTrace();
            System.exit(4);
        }
    }

    /**
     * Prints usage instructions.
     */
    private static void printUsage() {
        System.out.println("CSV Database Importer");
        System.out.println("====================");
        System.out.println();
        System.out.println("Usage: java -jar csv-database-importer.jar <csv-filename>");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar csv-database-importer.jar employee.csv");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  - Database connection: src/main/resources/database.properties");
        System.out.println("  - CSV files location: src/main/resources/");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  - Table name is derived from CSV filename (employee.csv -> employee table)");
        System.out.println("  - First row must contain column names");
        System.out.println("  - Date format can be configured in database.properties");
    }
}
