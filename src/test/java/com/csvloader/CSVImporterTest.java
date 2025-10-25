package com.csvloader;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVImporter class using PostgreSQL.
 *
 * Prerequisites:
 * - PostgreSQL must be running
 * - Test database 'testdb' must exist
 * - Update src/test/resources/test-database.properties with correct credentials
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CSVImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(CSVImporterTest.class);
    private static DatabaseConnection dbConnection;
    private static CSVImporter csvImporter;

    @BeforeAll
    static void setup() throws Exception {
        logger.info("========== CSVImporterTest - BeforeAll Setup ==========");
        // Load test properties
        Properties testProperties = new Properties();
        try (InputStream input = CSVImporterTest.class
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

        logger.info("Initializing database connection and CSV importer");
        dbConnection = new DatabaseConnection(testProperties);
        csvImporter = new CSVImporter(dbConnection);

        // Create test table
        logger.info("Creating test table 'employee'");
        createTestTable();
        logger.info("Test setup completed successfully");
    }

    @AfterAll
    static void teardown() throws Exception {
        logger.info("========== CSVImporterTest - AfterAll Teardown ==========");
        // Drop test table
        //logger.info("Dropping test table 'employee'");
        //dropTestTable();
        logger.info("Test teardown completed successfully");
    }

    @BeforeEach
    void clearTestData() throws Exception {
        logger.debug("BeforeEach - Clearing test data from employee table");
        // Clear data before each test
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            int deletedRows = stmt.executeUpdate("DELETE FROM employee");
            logger.debug("Deleted {} rows from employee table", deletedRows);
        }
    }

    private static void createTestTable() throws SQLException {
        logger.debug("Creating employee table");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Drop table if exists
            logger.debug("Dropping existing employee table if it exists");
            stmt.executeUpdate("DROP TABLE IF EXISTS employee");

            // Create employee table matching the CSV structure
            String createTableSQL = """
                CREATE TABLE employee (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    department VARCHAR(50),
                    hire_date DATE,
                    salary NUMERIC(10, 2)
                )
                """;
            stmt.executeUpdate(createTableSQL);
            logger.debug("Employee table created successfully");
        }
    }

    private static void dropTestTable() throws SQLException {
        logger.debug("Dropping employee table");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS employee");
            logger.debug("Employee table dropped successfully");
        }
    }

    @Test
    @Order(1)
    void testImportCSVFromFile() throws Exception {
        logger.info("TEST: testImportCSVFromFile - Starting");
        // Act: Import the employee.csv file from resources
        logger.debug("Importing employee.csv file");
        int rowCount = csvImporter.importCSV("employee.csv");
        logger.info("Imported {} rows from CSV file", rowCount);

        // Assert: Should import 5 rows
        assertEquals(5, rowCount, "Should import 5 employee records");

        // Verify data in database
        logger.debug("Verifying data in database");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee")) {

            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1), "Database should contain 5 records");
            logger.info("TEST: testImportCSVFromFile - PASSED");
        }
    }

    @Test
    @Order(2)
    void testImportCSVFromInputStream() throws Exception {
        logger.info("TEST: testImportCSVFromInputStream - Starting");
        // Arrange: Create CSV data
        String csvData = """
            id,name,email,department,hire_date,salary
            10,Test User,test@example.com,IT,2024-01-01,50000.00
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act: Import from stream
        logger.debug("Importing CSV from InputStream");
        int rowCount = csvImporter.importCSV(inputStream, "employee");
        logger.info("Imported {} rows from InputStream", rowCount);

        // Assert
        assertEquals(1, rowCount, "Should import 1 record");

        // Verify data
        logger.debug("Verifying imported data for id=10");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM employee WHERE id = 10")) {

            assertTrue(rs.next(), "Should find the imported record");
            assertEquals("Test User", rs.getString("name"));
            assertEquals("test@example.com", rs.getString("email"));
            assertEquals("IT", rs.getString("department"));
            assertEquals(50000.00, rs.getDouble("salary"), 0.01);
            logger.info("TEST: testImportCSVFromInputStream - PASSED");
        }
    }

    @Test
    @Order(3)
    void testImportWithDateFormats() throws Exception {
        logger.info("TEST: testImportWithDateFormats - Starting");
        // Arrange: CSV with various date values
        String csvData = """
            id,name,email,department,hire_date,salary
            20,Date Test 1,date1@test.com,IT,2024-01-15,60000
            21,Date Test 2,date2@test.com,HR,2023-12-31,55000
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act
        logger.debug("Importing CSV with date values");
        int rowCount = csvImporter.importCSV(inputStream, "employee");
        logger.info("Imported {} records with dates", rowCount);

        // Assert
        assertEquals(2, rowCount, "Should import 2 records with dates");

        // Verify dates are correctly parsed
        logger.debug("Verifying date parsing");
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, hire_date FROM employee WHERE id IN (20, 21) ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals(20, rs.getInt("id"));
            assertNotNull(rs.getDate("hire_date"), "Date should not be null");

            assertTrue(rs.next());
            assertEquals(21, rs.getInt("id"));
            assertNotNull(rs.getDate("hire_date"), "Date should not be null");
            logger.info("TEST: testImportWithDateFormats - PASSED");
        }
    }

    @Test
    @Order(4)
    void testImportWithNullValues() throws Exception {
        // Arrange: CSV with null/empty values
        String csvData = """
            id,name,email,department,hire_date,salary
            30,Null Test,,Engineering,,45000
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act
        int rowCount = csvImporter.importCSV(inputStream, "employee");

        // Assert
        assertEquals(1, rowCount, "Should import record with null values");

        // Verify nulls are handled correctly
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM employee WHERE id = 30")) {

            assertTrue(rs.next());
            assertEquals("Null Test", rs.getString("name"));
            assertNull(rs.getString("email"), "Email should be null");
            assertNull(rs.getDate("hire_date"), "Hire date should be null");
            assertEquals(45000, rs.getDouble("salary"), 0.01);
        }
    }

    @Test
    @Order(5)
    void testImportWithVariousDataTypes() throws Exception {
        // Arrange: CSV with different numeric types
        String csvData = """
            id,name,email,department,hire_date,salary
            40,Type Test,type@test.com,Finance,2024-06-15,99999.99
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act
        int rowCount = csvImporter.importCSV(inputStream, "employee");

        // Assert
        assertEquals(1, rowCount);

        // Verify data types
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM employee WHERE id = 40")) {

            assertTrue(rs.next());
            assertEquals(40, rs.getInt("id"), "Integer should be parsed correctly");
            assertEquals(99999.99, rs.getDouble("salary"), 0.01,
                "Decimal should be parsed correctly");
            assertEquals("Type Test", rs.getString("name"));
        }
    }

    @Test
    @Order(6)
    void testImportEmptyCSV() {
        // Arrange: Empty CSV
        String csvData = "id,name,email,department,hire_date,salary\n";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act
        assertDoesNotThrow(() -> {
            int rowCount = csvImporter.importCSV(inputStream, "employee");
            assertEquals(0, rowCount, "Should import 0 rows for empty CSV");
        });
    }

    @Test
    @Order(7)
    void testImportCSVWithMismatchedColumns() {
        // Arrange: CSV with wrong number of columns
        String csvData = """
            id,name,email,department,hire_date,salary
            50,Test,test@example.com,IT
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act & Assert: Should throw IOException
        IOException exception = assertThrows(IOException.class, () -> {
            csvImporter.importCSV(inputStream, "employee");
        });

        assertTrue(exception.getMessage().contains("expected"),
            "Exception should mention column count mismatch");
    }

    @Test
    @Order(8)
    void testImportCSVWithInvalidDate() {
        // Arrange: CSV with invalid date format
        String csvData = """
            id,name,email,department,hire_date,salary
            60,Invalid Date,invalid@test.com,IT,not-a-date,50000
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act & Assert: Should throw SQLException
        SQLException exception = assertThrows(SQLException.class, () -> {
            csvImporter.importCSV(inputStream, "employee");
        });

        assertTrue(exception.getMessage().contains("date"),
            "Exception should mention date format issue");
    }

    @Test
    @Order(9)
    void testImportCSVWithInvalidNumber() {
        // Arrange: CSV with invalid number
        String csvData = """
            id,name,email,department,hire_date,salary
            not-a-number,Invalid Number,invalid@test.com,IT,2024-01-01,50000
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act & Assert: Should throw SQLException
        SQLException exception = assertThrows(SQLException.class, () -> {
            csvImporter.importCSV(inputStream, "employee");
        });

        assertTrue(exception.getMessage().contains("number"),
            "Exception should mention number format issue");
    }

    @Test
    @Order(10)
    void testImportNonExistentFile() {
        // Act & Assert: Should throw IOException
        IOException exception = assertThrows(IOException.class, () -> {
            csvImporter.importCSV("nonexistent.csv");
        });

        assertTrue(exception.getMessage().contains("not found"),
            "Exception should indicate file not found");
    }

    @Test
    @Order(11)
    void testGetDatePattern() {
        // Act
        String datePattern = csvImporter.getDatePattern();

        // Assert
        assertNotNull(datePattern, "Date pattern should not be null");
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", datePattern, "Should use ISO 8601 datetime pattern");
    }

    @Test
    @Order(12)
    void testImportMultipleRows() throws Exception {
        // Arrange: CSV with multiple rows
        String csvData = """
            id,name,email,department,hire_date,salary
            70,User One,user1@test.com,IT,2024-01-01,60000
            71,User Two,user2@test.com,HR,2024-02-01,65000
            72,User Three,user3@test.com,Sales,2024-03-01,70000
            73,User Four,user4@test.com,Marketing,2024-04-01,75000
            """;

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        // Act
        int rowCount = csvImporter.importCSV(inputStream, "employee");

        // Assert
        assertEquals(4, rowCount, "Should import 4 records");

        // Verify all rows are inserted
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM employee WHERE id >= 70 AND id <= 73")) {

            assertTrue(rs.next());
            assertEquals(4, rs.getInt(1), "Should have 4 records in database");
        }
    }
}
