package com.csvloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Main application class for CSV Database Importer.
 * Usage:
 *   java -jar csv-database-importer.jar                    (imports all files from config)
 *   java -jar csv-database-importer.jar <csv-filename>     (imports specific file)
 *
 * Example:
 *   java -jar csv-database-importer.jar
 *   java -jar csv-database-importer.jar employee.csv
 */
public class CSVImporterApp {

    private static final Logger logger = LoggerFactory.getLogger(CSVImporterApp.class);

    public static void main(String[] args) {
        logger.info("========== CSV Database Importer Starting ==========");

        try {
            // Initialize configurations
            logger.info("Loading configurations");
            DatabaseConnection dbConnection = new DatabaseConnection();
            ApplicationConfig appConfig = new ApplicationConfig();

            // Create CSV importer
            CSVImporter importer = new CSVImporter(dbConnection, appConfig);

            logger.info("Date format configured as: {}", importer.getDatePattern());
            System.out.println("Date format configured as: " + importer.getDatePattern());

            // Determine which files to import
            List<String> filesToImport;
            if (args.length == 0) {
                // No arguments - import all files from configuration
                logger.info("No file specified, loading files from application.properties");
                filesToImport = appConfig.getCsvFiles();

                if (filesToImport.isEmpty()) {
                    logger.error("No CSV files configured in application.properties");
                    System.err.println("ERROR: No CSV files configured in application.properties");
                    System.err.println("Please add csv.files property with comma-separated file list");
                    printUsage();
                    System.exit(1);
                }

                logger.info("Files to import from configuration: {}", filesToImport);
                System.out.println("Importing " + filesToImport.size() + " file(s) from configuration:");
                filesToImport.forEach(file -> System.out.println("  - " + file));
            } else {
                // File specified as argument - import only that file
                String csvFileName = args[0];
                logger.info("Importing specific file: {}", csvFileName);
                filesToImport = List.of(csvFileName);
                System.out.println("Importing file: " + csvFileName);
            }

            // Import all files
            int totalRowsImported = 0;
            int filesImported = 0;

            for (String csvFile : filesToImport) {
                try {
                    logger.info("Starting import of file: {}", csvFile);
                    System.out.println("\nImporting: " + csvFile);

                    int rowCount = importer.importCSV(csvFile);
                    totalRowsImported += rowCount;
                    filesImported++;

                    logger.info("Successfully imported {} rows from {}", rowCount, csvFile);
                    System.out.println("  ✓ Imported " + rowCount + " rows");

                } catch (IOException e) {
                    logger.error("Failed to read CSV file: {}", csvFile, e);
                    System.err.println("  ✗ ERROR: Failed to read " + csvFile);
                    System.err.println("    " + e.getMessage());

                } catch (SQLException e) {
                    logger.error("Database operation failed for file: {}", csvFile, e);
                    System.err.println("  ✗ ERROR: Database operation failed for " + csvFile);
                    System.err.println("    " + e.getMessage());
                }
            }

            // Summary
            logger.info("Import completed: {} files, {} total rows", filesImported, totalRowsImported);
            System.out.println("\n========================================");
            System.out.println("Import Summary:");
            System.out.println("  Files processed: " + filesImported + "/" + filesToImport.size());
            System.out.println("  Total rows imported: " + totalRowsImported);
            System.out.println("========================================");

            if (filesImported < filesToImport.size()) {
                logger.warn("Some files failed to import");
                System.exit(2);
            }

            // Export table headers if configured
            if (appConfig.getExportEnabled()) {
                logger.info("Export is enabled, starting table header export");
                System.out.println("\n========================================");
                System.out.println("CSV Export:");
                System.out.println("========================================");

                List<String> tablesToExport = appConfig.getExportTables();
                String outputDir = appConfig.getExportOutputDir();

                if (tablesToExport.isEmpty()) {
                    logger.warn("Export is enabled but no tables configured");
                    System.out.println("  ! Warning: Export enabled but no tables configured");
                    System.out.println("    Add export.tables property with comma-separated table names");
                } else {
                    logger.info("Exporting headers for {} tables to {}", tablesToExport.size(), outputDir);
                    System.out.println("Exporting headers for " + tablesToExport.size() + " table(s):");
                    tablesToExport.forEach(table -> System.out.println("  - " + table));
                    System.out.println("Output directory: " + outputDir);

                    try {
                        CSVExporter exporter = new CSVExporter(dbConnection, appConfig.getCsvDelimiter());
                        int exportedCount = exporter.exportTableHeaders(tablesToExport, outputDir);

                        logger.info("Successfully exported {} table headers", exportedCount);
                        System.out.println("\nExport Summary:");
                        System.out.println("  Tables exported: " + exportedCount + "/" + tablesToExport.size());
                        System.out.println("  Output location: " + outputDir);
                        System.out.println("========================================");

                        if (exportedCount < tablesToExport.size()) {
                            logger.warn("Some tables failed to export");
                        }

                    } catch (IOException e) {
                        logger.error("Export failed", e);
                        System.err.println("  ✗ ERROR: Export failed");
                        System.err.println("    " + e.getMessage());
                    }
                }
            } else {
                logger.debug("Export is disabled");
            }

            logger.info("CSV Database Importer completed successfully");
            System.exit(0);

        } catch (IOException e) {
            logger.error("Configuration error", e);
            System.err.println("ERROR: Failed to load configuration");
            System.err.println("Details: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);

        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
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
        System.out.println("Usage:");
        System.out.println("  java -jar csv-database-importer.jar                    (imports all files from config)");
        System.out.println("  java -jar csv-database-importer.jar <csv-filename>     (imports specific file)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar csv-database-importer.jar");
        System.out.println("  java -jar csv-database-importer.jar employee.csv");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  - Database connection: src/main/resources/database.properties");
        System.out.println("  - Application settings: src/main/resources/application.properties");
        System.out.println("  - CSV files location: src/main/resources/");
        System.out.println();
        System.out.println("application.properties:");
        System.out.println("  Import settings:");
        System.out.println("    - csv.files: Comma-separated list of CSV files to import");
        System.out.println("    - date.format: Date format pattern (e.g., yyyy-MM-dd)");
        System.out.println("    - csv.delimiter: CSV delimiter character");
        System.out.println("  Export settings:");
        System.out.println("    - export.enabled: Enable CSV export (true/false)");
        System.out.println("    - export.tables: Comma-separated list of tables to export");
        System.out.println("    - export.output.dir: Output directory for exported CSV files");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  - Table name is derived from CSV filename (employee.csv -> employee table)");
        System.out.println("  - First row must contain column names");
        System.out.println("  - All CSV files must exist in src/main/resources/");
        System.out.println("  - Export generates CSV files with column headers only");
    }
}
