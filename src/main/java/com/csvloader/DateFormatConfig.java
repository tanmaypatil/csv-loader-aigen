package com.csvloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles date parsing with configurable format.
 * Format can be customized in database.properties.
 */
public class DateFormatConfig {

    private final String datePattern;
    private final SimpleDateFormat dateFormat;

    /**
     * Creates DateFormatConfig with specified pattern.
     * @param datePattern Java SimpleDateFormat pattern (e.g., "yyyy-MM-dd")
     */
    public DateFormatConfig(String datePattern) {
        this.datePattern = datePattern;
        this.dateFormat = new SimpleDateFormat(datePattern);
        this.dateFormat.setLenient(false); // Strict parsing
    }

    /**
     * Gets the current date pattern.
     * @return date pattern string
     */
    public String getDatePattern() {
        return datePattern;
    }

    /**
     * Parses a date string using the configured format.
     * @param dateString string to parse
     * @return parsed Date object
     * @throws ParseException if date cannot be parsed
     */
    public Date parseDate(String dateString) throws ParseException {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return dateFormat.parse(dateString.trim());
    }

    /**
     * Attempts to parse a date, returns null if parsing fails.
     * Useful for optional date fields.
     * @param dateString string to parse
     * @return parsed Date or null
     */
    public Date parseDateSafe(String dateString) {
        try {
            return parseDate(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Formats a Date object to string.
     * @param date Date to format
     * @return formatted date string
     */
    public String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return dateFormat.format(date);
    }

    /**
     * Converts parsed Date to java.sql.Date for database insertion.
     * @param dateString string to parse
     * @return java.sql.Date object
     * @throws ParseException if date cannot be parsed
     */
    public java.sql.Date toSqlDate(String dateString) throws ParseException {
        Date date = parseDate(dateString);
        return date != null ? new java.sql.Date(date.getTime()) : null;
    }
}
