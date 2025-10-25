package com.csvloader;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Handles date and datetime parsing with configurable format using java.time API.
 * Format can be customized in application.properties.
 *
 * Default format: ISO 8601 with milliseconds (e.g., "2025-10-25T14:30:00.000Z")
 * Supports flexible timezone parsing: Z, +00:00, +05:30, etc.
 * For date-only values, time defaults to 00:00:00.000 and timezone defaults to UTC (Z)
 */
public class DateFormatConfig {

    private final String datetimePattern;
    private final DateTimeFormatter datetimeFormatter;
    private final DateTimeFormatter dateOnlyFormatter;
    private final ZoneId defaultZone;

    /**
     * Creates DateFormatConfig with specified pattern.
     * @param datetimePattern DateTimeFormatter pattern (e.g., "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
     *                        where XXX handles timezone: Z, +00:00, +05:30, etc.
     */
    public DateFormatConfig(String datetimePattern) {
        this.datetimePattern = datetimePattern;
        this.datetimeFormatter = DateTimeFormatter.ofPattern(datetimePattern);

        // Date-only format (extracts just the date portion)
        this.dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Default timezone is UTC
        this.defaultZone = ZoneId.of("UTC");
    }

    /**
     * Gets the current datetime pattern.
     * @return datetime pattern string
     */
    public String getDatePattern() {
        return datetimePattern;
    }

    /**
     * Parses a datetime string with full date, time, and timezone.
     * @param datetimeString string to parse (e.g., "2024-01-15 14:30:00 UTC")
     * @return parsed ZonedDateTime object
     * @throws DateTimeParseException if datetime cannot be parsed
     */
    public ZonedDateTime parseDateTime(String datetimeString) {
        if (datetimeString == null || datetimeString.trim().isEmpty()) {
            return null;
        }
        return ZonedDateTime.parse(datetimeString.trim(), datetimeFormatter);
    }

    /**
     * Parses a date-only string and returns ZonedDateTime with time 00:00:00 UTC.
     * Used when CSV contains only date without time.
     * @param dateString date-only string (e.g., "2024-01-15")
     * @return ZonedDateTime with time set to 00:00:00 UTC
     * @throws DateTimeParseException if date cannot be parsed
     */
    public ZonedDateTime parseDateOnly(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        LocalDate date = LocalDate.parse(dateString.trim(), dateOnlyFormatter);
        return date.atStartOfDay(defaultZone);
    }

    /**
     * Smart parser that handles both full datetime and date-only formats.
     * First tries to parse as full datetime, falls back to date-only format.
     * @param dateTimeString string to parse
     * @return ZonedDateTime object
     * @throws DateTimeParseException if string cannot be parsed in either format
     */
    public ZonedDateTime parseFlexible(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateTimeString.trim();

        // Try full datetime format first
        try {
            return ZonedDateTime.parse(trimmed, datetimeFormatter);
        } catch (DateTimeParseException e) {
            // Fall back to date-only format (adds 00:00:00 UTC)
            try {
                LocalDate date = LocalDate.parse(trimmed, dateOnlyFormatter);
                return date.atStartOfDay(defaultZone);
            } catch (DateTimeParseException ex) {
                throw new DateTimeParseException(
                    "Failed to parse as datetime (" + datetimePattern +
                    ") or date-only (yyyy-MM-dd): " + trimmed,
                    trimmed, 0);
            }
        }
    }

    /**
     * Converts ZonedDateTime to java.sql.Date for DATE columns.
     * @param zonedDateTime ZonedDateTime to convert
     * @return java.sql.Date object (date only, no time)
     */
    public java.sql.Date toSqlDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return java.sql.Date.valueOf(zonedDateTime.toLocalDate());
    }

    /**
     * Converts ZonedDateTime to java.sql.Timestamp for TIMESTAMP columns.
     * @param zonedDateTime ZonedDateTime to convert
     * @return java.sql.Timestamp object (includes date, time, and timezone)
     */
    public java.sql.Timestamp toSqlTimestamp(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return java.sql.Timestamp.from(zonedDateTime.toInstant());
    }

    /**
     * Formats a ZonedDateTime to string using configured pattern.
     * @param zonedDateTime ZonedDateTime to format
     * @return formatted datetime string
     */
    public String formatDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return datetimeFormatter.format(zonedDateTime);
    }

    /**
     * Gets the default timezone (UTC).
     * @return default ZoneId
     */
    public ZoneId getDefaultZone() {
        return defaultZone;
    }
}
