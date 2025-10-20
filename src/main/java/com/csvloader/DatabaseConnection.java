package com.csvloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple database connection utility.
 * Loads configuration from database.properties file.
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String CONFIG_FILE = "database.properties";
    private Properties properties;

    /**
     * Creates a new DatabaseConnection and loads configuration.
     * @throws IOException if configuration file cannot be read
     */
    public DatabaseConnection() throws IOException {
        logger.debug("Initializing DatabaseConnection");
        this.properties = new Properties();
        loadConfiguration();
        logger.info("DatabaseConnection initialized successfully");
    }

    /**
     * Creates a DatabaseConnection with custom properties (useful for testing).
     * @param properties custom database properties
     */
    public DatabaseConnection(Properties properties) {
        this.properties = properties;
    }

    /**
     * Loads database configuration from properties file.
     */
    private void loadConfiguration() throws IOException {
        logger.debug("Loading configuration from {}", CONFIG_FILE);
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.error("Configuration file not found: {}", CONFIG_FILE);
                throw new IOException("Unable to find " + CONFIG_FILE);
            }
            properties.load(input);
            logger.debug("Configuration loaded successfully");
        }
    }

    /**
     * Establishes and returns a database connection.
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");
        String driver = properties.getProperty("db.driver");

        logger.debug("Attempting to establish database connection to: {}", url);

        // Validate required properties
        if (url == null || username == null || password == null) {
            logger.error("Missing required database configuration properties");
            throw new SQLException("Missing required database configuration");
        }

        // Load driver (optional for JDBC 4.0+, but included for clarity)
        if (driver != null) {
            try {
                logger.debug("Loading database driver: {}", driver);
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                logger.error("Database driver not found: {}", driver, e);
                throw new SQLException("Database driver not found: " + driver, e);
            }
        }

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            logger.info("Database connection established successfully");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to establish database connection to: {}", url, e);
            throw e;
        }
    }

    /**
     * Gets a property value from configuration.
     * @param key property key
     * @return property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a property value with a default.
     * @param key property key
     * @param defaultValue default value if key not found
     * @return property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
