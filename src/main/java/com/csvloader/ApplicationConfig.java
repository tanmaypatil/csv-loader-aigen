package com.csvloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Application configuration utility.
 * Loads configuration from application.properties file.
 */
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private Properties properties;

    /**
     * Creates a new ApplicationConfig and loads configuration.
     * @throws IOException if configuration file cannot be read
     */
    public ApplicationConfig() throws IOException {
        logger.debug("Initializing ApplicationConfig");
        this.properties = new Properties();
        loadConfiguration();
        logger.info("ApplicationConfig initialized successfully");
    }

    /**
     * Creates an ApplicationConfig with custom properties (useful for testing).
     * @param properties custom application properties
     */
    public ApplicationConfig(Properties properties) {
        this.properties = properties;
    }

    /**
     * Loads application configuration from properties file.
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
     * Gets the date format pattern.
     * @return date format pattern (e.g., "yyyy-MM-dd")
     */
    public String getDateFormat() {
        return properties.getProperty("date.format", "yyyy-MM-dd");
    }

    /**
     * Gets the CSV delimiter.
     * @return CSV delimiter (default: ",")
     */
    public String getCsvDelimiter() {
        return properties.getProperty("csv.delimiter", ",");
    }

    /**
     * Gets whether CSV has header row.
     * @return true if CSV has header row
     */
    public boolean getCsvHasHeader() {
        return Boolean.parseBoolean(properties.getProperty("csv.has.header", "true"));
    }

    /**
     * Gets the list of CSV files to import.
     * @return list of CSV filenames
     */
    public List<String> getCsvFiles() {
        String filesProperty = properties.getProperty("csv.files", "");
        if (filesProperty == null || filesProperty.trim().isEmpty()) {
            logger.warn("No CSV files configured in csv.files property");
            return new ArrayList<>();
        }

        // Split by comma and trim whitespace
        List<String> files = Arrays.stream(filesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        logger.debug("Configured CSV files: {}", files);
        return files;
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
