package com.csvloader;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVExporter class.
 *
 * Prerequisites:
 * - PostgreSQL must be running
 * - Test database must exist
 * - Update src/test/resources/test-database.properties with correct credentials
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CSVExporterTest {

    private static final Logger logger = LoggerFactory.getLogger(CSVExporterTest.class);
    private static DatabaseConnection dbConnection;
    private static CSVExporter csvExporter;
    private static String testOutputDir;

    @BeforeAll
    static void setup() throws Exception {
        logger.info("========== CSVExporterTest - BeforeAll Setup ==========");

        // Load test properties
        Properties testProperties = new Properties();
        try (InputStream input = CSVExporterTest.class
                .getClassLoader()
                .getResourceAsStream("test-database.properties")) {
            if (input != null) {
                testProperties.load(input);
                logger.info("Test properties loaded successfully");
            } else {
                logger.error("test-database.properties not found");
                fail("test-database.properties not found");
            }
        }

        logger.info("Initializing database connection and CSV exporter");
        dbConnection = new DatabaseConnection(testProperties);
        csvExporter = new CSVExporter(dbConnection, ",");

        // Create test output directory
        testOutputDir = "target/test-output-csv";
        File dir = new File(testOutputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Create test tables
        logger.info("Creating test tables");
        createTestTables();
        logger.info("Test setup completed successfully");
    }

    @AfterAll
    static void teardown() throws Exception {
        logger.info("========== CSVExporterTest - AfterAll Teardown ==========");

        // Clean up test output directory
        File dir = new File(testOutputDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        logger.info("Test teardown completed successfully");
    }

    @BeforeEach
    void cleanOutputDir() {
        logger.debug("BeforeEach - Cleaning test output directory");
        File dir = new File(testOutputDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    private static void createTestTables() throws SQLException {
        logger.debug("Creating test tables");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Drop tables if they exist
            logger.debug("Dropping existing test tables if they exist");
            stmt.executeUpdate("DROP TABLE IF EXISTS employee");
            stmt.executeUpdate("DROP TABLE IF EXISTS department");
            stmt.executeUpdate("DROP TABLE IF EXISTS project");

            // Create employee table
            String createEmployeeTable = """
                CREATE TABLE employee (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    department VARCHAR(50),
                    hire_date DATE,
                    salary NUMERIC(10, 2)
                )
                """;
            stmt.executeUpdate(createEmployeeTable);
            logger.debug("Created employee table");

            // Create department table
            String createDepartmentTable = """
                CREATE TABLE department (
                    dept_id INTEGER PRIMARY KEY,
                    dept_name VARCHAR(100) NOT NULL,
                    location VARCHAR(100)
                )
                """;
            stmt.executeUpdate(createDepartmentTable);
            logger.debug("Created department table");

            // Create project table
            String createProjectTable = """
                CREATE TABLE project (
                    project_id INTEGER PRIMARY KEY,
                    project_name VARCHAR(200) NOT NULL,
                    start_date DATE,
                    budget NUMERIC(15, 2)
                )
                """;
            stmt.executeUpdate(createProjectTable);
            logger.debug("Created project table");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test exporting single table header")
    void testExportSingleTable() throws IOException {
        logger.info("TEST: Export single table header");

        List<String> tables = List.of("employee");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(1, exportedCount, "Should export 1 table");

        // Verify file was created
        File csvFile = new File(testOutputDir, "employee.csv");
        assertTrue(csvFile.exists(), "employee.csv should exist");

        // Verify content
        String content = Files.readString(csvFile.toPath()).trim();
        logger.debug("Exported content: {}", content);

        // Should contain all column names
        assertTrue(content.contains("id"), "Should contain 'id' column");
        assertTrue(content.contains("name"), "Should contain 'name' column");
        assertTrue(content.contains("email"), "Should contain 'email' column");
        assertTrue(content.contains("department"), "Should contain 'department' column");
        assertTrue(content.contains("hire_date"), "Should contain 'hire_date' column");
        assertTrue(content.contains("salary"), "Should contain 'salary' column");

        // Count lines (should be just 1 header line + 1 newline = 2 lines, but trim removes trailing newline)
        String[] lines = Files.readString(csvFile.toPath()).split("\n");
        assertEquals(1, lines.length, "Should have only 1 line (header)");
    }

    @Test
    @Order(2)
    @DisplayName("Test exporting multiple table headers")
    void testExportMultipleTables() throws IOException {
        logger.info("TEST: Export multiple table headers");

        List<String> tables = Arrays.asList("employee", "department", "project");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(3, exportedCount, "Should export 3 tables");

        // Verify all files were created
        File employeeFile = new File(testOutputDir, "employee.csv");
        File departmentFile = new File(testOutputDir, "department.csv");
        File projectFile = new File(testOutputDir, "project.csv");

        assertTrue(employeeFile.exists(), "employee.csv should exist");
        assertTrue(departmentFile.exists(), "department.csv should exist");
        assertTrue(projectFile.exists(), "project.csv should exist");

        // Verify department content
        String deptContent = Files.readString(departmentFile.toPath()).trim();
        assertTrue(deptContent.contains("dept_id"), "Department should contain 'dept_id'");
        assertTrue(deptContent.contains("dept_name"), "Department should contain 'dept_name'");
        assertTrue(deptContent.contains("location"), "Department should contain 'location'");

        // Verify project content
        String projectContent = Files.readString(projectFile.toPath()).trim();
        assertTrue(projectContent.contains("project_id"), "Project should contain 'project_id'");
        assertTrue(projectContent.contains("project_name"), "Project should contain 'project_name'");
        assertTrue(projectContent.contains("start_date"), "Project should contain 'start_date'");
        assertTrue(projectContent.contains("budget"), "Project should contain 'budget'");
    }

    @Test
    @Order(3)
    @DisplayName("Test exporting with custom delimiter")
    void testExportWithCustomDelimiter() throws IOException {
        logger.info("TEST: Export with custom delimiter");

        CSVExporter semicolonExporter = new CSVExporter(dbConnection, ";");
        List<String> tables = List.of("employee");
        int exportedCount = semicolonExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(1, exportedCount, "Should export 1 table");

        // Verify file content uses semicolon
        File csvFile = new File(testOutputDir, "employee.csv");
        String content = Files.readString(csvFile.toPath()).trim();
        logger.debug("Exported content with semicolon: {}", content);

        assertTrue(content.contains(";"), "Should use semicolon delimiter");
        assertFalse(content.contains(","), "Should not contain commas");
    }

    @Test
    @Order(4)
    @DisplayName("Test exporting non-existent table")
    void testExportNonExistentTable() throws IOException {
        logger.info("TEST: Export non-existent table");

        List<String> tables = List.of("nonexistent_table");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        // Should not throw exception, but should return 0 for failed export
        assertEquals(0, exportedCount, "Should export 0 tables (table doesn't exist)");

        // Verify no file was created
        File csvFile = new File(testOutputDir, "nonexistent_table.csv");
        assertFalse(csvFile.exists(), "nonexistent_table.csv should not exist");
    }

    @Test
    @Order(5)
    @DisplayName("Test exporting with empty table list")
    void testExportEmptyList() throws IOException {
        logger.info("TEST: Export with empty table list");

        List<String> tables = List.of();
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(0, exportedCount, "Should export 0 tables");
    }

    @Test
    @Order(6)
    @DisplayName("Test exporting with null table list")
    void testExportNullList() throws IOException {
        logger.info("TEST: Export with null table list");

        int exportedCount = csvExporter.exportTableHeaders(null, testOutputDir);

        assertEquals(0, exportedCount, "Should export 0 tables");
    }

    @Test
    @Order(7)
    @DisplayName("Test output directory creation")
    void testOutputDirectoryCreation() throws IOException {
        logger.info("TEST: Output directory creation");

        String newDir = testOutputDir + "/subdirectory/nested";
        File dir = new File(newDir);
        if (dir.exists()) {
            dir.delete();
        }

        List<String> tables = List.of("employee");
        int exportedCount = csvExporter.exportTableHeaders(tables, newDir);

        assertEquals(1, exportedCount, "Should export 1 table");
        assertTrue(new File(newDir).exists(), "Output directory should be created");
        assertTrue(new File(newDir, "employee.csv").exists(), "CSV file should exist in new directory");
    }

    @Test
    @Order(8)
    @DisplayName("Test exporting with mixed valid and invalid tables")
    void testExportMixedTables() throws IOException {
        logger.info("TEST: Export mixed valid and invalid tables");

        List<String> tables = Arrays.asList("employee", "nonexistent", "department");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        // Should export only valid tables (2 out of 3)
        assertEquals(2, exportedCount, "Should export 2 valid tables");

        assertTrue(new File(testOutputDir, "employee.csv").exists(), "employee.csv should exist");
        assertTrue(new File(testOutputDir, "department.csv").exists(), "department.csv should exist");
        assertFalse(new File(testOutputDir, "nonexistent.csv").exists(), "nonexistent.csv should not exist");
    }

    @Test
    @Order(9)
    @DisplayName("Test column order preservation")
    void testColumnOrderPreservation() throws IOException {
        logger.info("TEST: Column order preservation");

        List<String> tables = List.of("employee");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(1, exportedCount, "Should export 1 table");

        // Verify column order matches table definition
        File csvFile = new File(testOutputDir, "employee.csv");
        String content = Files.readString(csvFile.toPath()).trim();
        String[] columns = content.split(",");

        // According to our table definition, columns should be in this order
        assertEquals("id", columns[0], "First column should be 'id'");
        assertEquals("name", columns[1], "Second column should be 'name'");
        assertEquals("email", columns[2], "Third column should be 'email'");
        assertEquals("department", columns[3], "Fourth column should be 'department'");
        assertEquals("hire_date", columns[4], "Fifth column should be 'hire_date'");
        assertEquals("salary", columns[5], "Sixth column should be 'salary'");
    }

    @Test
    @Order(10)
    @DisplayName("Test exporting table with spaces in name (trimmed)")
    void testExportTableWithSpaces() throws IOException {
        logger.info("TEST: Export table with spaces in name");

        List<String> tables = List.of(" employee ", "  department  ");
        int exportedCount = csvExporter.exportTableHeaders(tables, testOutputDir);

        assertEquals(2, exportedCount, "Should export 2 tables (trimmed)");

        assertTrue(new File(testOutputDir, "employee.csv").exists(), "employee.csv should exist");
        assertTrue(new File(testOutputDir, "department.csv").exists(), "department.csv should exist");
    }
}
