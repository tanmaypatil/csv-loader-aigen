package com.csvloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseConnection class using PostgreSQL.
 *
 * Prerequisites:
 * - PostgreSQL must be running
 * - Test database 'testdb' must exist
 * - Update src/test/resources/test-database.properties with correct credentials
 */
class DatabaseConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);
    private static Properties testProperties;

    @BeforeAll
    static void setup() throws IOException {
        logger.info("Setting up DatabaseConnectionTest");
        // Load test properties
        testProperties = new Properties();
        try (InputStream input = DatabaseConnectionTest.class
                .getClassLoader()
                .getResourceAsStream("test-database.properties")) {
            if (input != null) {
                testProperties.load(input);
                logger.info("Test properties loaded successfully");
            } else {
                logger.warn("test-database.properties not found");
            }
        }
    }

    @Test
    void testGetConnection() throws Exception {
        logger.info("Running testGetConnection");
        // Arrange
        DatabaseConnection dbConn = new DatabaseConnection(testProperties);

        // Act: Get connection
        Connection conn = dbConn.getConnection();
        logger.debug("Connection obtained: {}", conn);

        // Assert: Connection should be valid
        assertNotNull(conn, "Connection should not be null");
        assertFalse(conn.isClosed(), "Connection should be open");

        // Verify it's PostgreSQL
        String dbProduct = conn.getMetaData().getDatabaseProductName();
        assertTrue(dbProduct.toLowerCase().contains("postgresql"),
            "Should be connected to PostgreSQL, got: " + dbProduct);

        // Cleanup
        conn.close();
        assertTrue(conn.isClosed(), "Connection should be closed");
    }

    @Test
    void testMultipleConnections() throws Exception {
        // Arrange
        DatabaseConnection dbConn = new DatabaseConnection(testProperties);

        // Act: Get multiple connections
        Connection conn1 = dbConn.getConnection();
        Connection conn2 = dbConn.getConnection();

        // Assert: Both should be valid and different
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotSame(conn1, conn2, "Should create separate connection instances");

        // Cleanup
        conn1.close();
        conn2.close();
    }

    @Test
    void testMissingConfiguration() {
        // Arrange: Create properties missing required fields
        Properties invalidProps = new Properties();
        invalidProps.setProperty("db.url", "jdbc:postgresql://localhost:5432/testdb");
        // Missing username and password

        DatabaseConnection dbConn = new DatabaseConnection(invalidProps);

        // Act & Assert: Should throw SQLException
        SQLException exception = assertThrows(SQLException.class, () -> {
            dbConn.getConnection();
        });

        assertTrue(exception.getMessage().contains("Missing required database configuration"),
            "Exception message should indicate missing configuration");
    }

    @Test
    void testGetProperty() {
        // Arrange
        DatabaseConnection dbConn = new DatabaseConnection(testProperties);

        // Act & Assert
        assertNotNull(dbConn.getProperty("db.url"), "Should get db.url property");
        assertNotNull(dbConn.getProperty("db.username"), "Should get db.username property");
        assertTrue(dbConn.getProperty("db.url").contains("postgresql"),
            "URL should contain postgresql");
    }

    @Test
    void testGetPropertyWithDefault() {
        // Arrange
        DatabaseConnection dbConn = new DatabaseConnection(testProperties);

        // Act & Assert
        assertEquals("yyyy-MM-dd",
            dbConn.getProperty("date.format", "yyyy-MM-dd"),
            "Should get date format or default");
        assertEquals("default-value",
            dbConn.getProperty("nonexistent.key", "default-value"),
            "Should return default for missing key");
    }

    @Test
    void testInvalidConnectionUrl() {
        // Arrange
        Properties invalidProps = new Properties();
        invalidProps.setProperty("db.url", "jdbc:postgresql://invalid-host:9999/invalid");
        invalidProps.setProperty("db.username", "invalid");
        invalidProps.setProperty("db.password", "invalid");
        invalidProps.setProperty("db.driver", "org.postgresql.Driver");

        DatabaseConnection dbConn = new DatabaseConnection(invalidProps);

        // Act & Assert: Should throw SQLException
        assertThrows(SQLException.class, () -> {
            dbConn.getConnection();
        }, "Should throw exception for invalid connection");
    }

    @Test
    void testInvalidDriver() {
        // Arrange
        Properties invalidProps = new Properties();
        invalidProps.setProperty("db.url", "jdbc:postgresql://localhost:5432/testdb");
        invalidProps.setProperty("db.username", "postgres");
        invalidProps.setProperty("db.password", "postgres");
        invalidProps.setProperty("db.driver", "com.invalid.NonExistentDriver");

        DatabaseConnection dbConn = new DatabaseConnection(invalidProps);

        // Act & Assert: Should throw SQLException due to missing driver
        SQLException exception = assertThrows(SQLException.class, () -> {
            dbConn.getConnection();
        });

        assertTrue(exception.getMessage().contains("Database driver not found"),
            "Exception should indicate driver not found");
    }
}
