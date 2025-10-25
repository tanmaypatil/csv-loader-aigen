package com.csvloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateFormatConfig class.
 * Tests ISO 8601 datetime parsing with timezone support.
 */
class DateFormatConfigTest {

    private DateFormatConfig dateConfig;

    @BeforeEach
    void setUp() {
        // Use ISO 8601 format with milliseconds (XXX for timezone with colon)
        dateConfig = new DateFormatConfig("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    @Test
    void testParseFullDateTimeWithUTC() {
        // Arrange
        String dateTimeString = "2025-10-25T14:30:00.000Z";

        // Act
        ZonedDateTime result = dateConfig.parseDateTime(dateTimeString);

        // Assert
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
        // Z and UTC are equivalent
        assertEquals(ZoneId.of("Z"), result.getZone());
    }

    @Test
    void testParseFullDateTimeWithOffset() {
        // Arrange - Indian Standard Time (IST = UTC+05:30)
        String dateTimeString = "2025-10-25T14:30:00.000+05:30";

        // Act
        ZonedDateTime result = dateConfig.parseDateTime(dateTimeString);

        // Assert
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
        // Verify timezone offset
        assertEquals("+05:30", result.getOffset().toString());
    }

    @Test
    void testParseDateOnlyDefaultsToUTC() {
        // Arrange
        String dateString = "2025-10-25";

        // Act
        ZonedDateTime result = dateConfig.parseDateOnly(dateString);

        // Assert
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
        // parseDateOnly uses UTC zone
        assertEquals(ZoneId.of("UTC"), result.getZone());
    }

    @Test
    void testParseFlexibleWithFullDateTime() {
        // Arrange
        String dateTimeString = "2025-10-25T14:30:00.000Z";

        // Act
        ZonedDateTime result = dateConfig.parseFlexible(dateTimeString);

        // Assert
        assertNotNull(result);
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    void testParseFlexibleWithDateOnlyFallback() {
        // Arrange
        String dateString = "2025-10-25";

        // Act
        ZonedDateTime result = dateConfig.parseFlexible(dateString);

        // Assert
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(0, result.getHour()); // Defaults to 00:00:00
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(ZoneId.of("UTC"), result.getZone());
    }

    @Test
    void testParseFlexibleWithNegativeOffset() {
        // Arrange - Eastern Standard Time (EST = UTC-05:00)
        String dateTimeString = "2025-10-25T14:30:00.000-05:00";

        // Act
        ZonedDateTime result = dateConfig.parseFlexible(dateTimeString);

        // Assert
        assertNotNull(result);
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals("-05:00", result.getOffset().toString());
    }

    @Test
    void testParseNullOrEmpty() {
        // Act & Assert
        assertNull(dateConfig.parseDateTime(null));
        assertNull(dateConfig.parseDateTime(""));
        assertNull(dateConfig.parseDateTime("   "));

        assertNull(dateConfig.parseDateOnly(null));
        assertNull(dateConfig.parseDateOnly(""));

        assertNull(dateConfig.parseFlexible(null));
        assertNull(dateConfig.parseFlexible(""));
    }

    @Test
    void testParseInvalidFormat() {
        // Arrange
        String invalidDateTime = "25-10-2025 14:30:00"; // Wrong format

        // Act & Assert
        assertThrows(DateTimeParseException.class, () -> {
            dateConfig.parseDateTime(invalidDateTime);
        });
    }

    @Test
    void testParseFlexibleInvalidFormat() {
        // Arrange
        String invalidDateTime = "not-a-date";

        // Act & Assert
        DateTimeParseException exception = assertThrows(DateTimeParseException.class, () -> {
            dateConfig.parseFlexible(invalidDateTime);
        });

        assertTrue(exception.getMessage().contains("Failed to parse"));
    }

    @Test
    void testToSqlDate() {
        // Arrange
        String dateTimeString = "2025-10-25T14:30:00.000Z";
        ZonedDateTime zonedDateTime = dateConfig.parseDateTime(dateTimeString);

        // Act
        java.sql.Date sqlDate = dateConfig.toSqlDate(zonedDateTime);

        // Assert
        assertNotNull(sqlDate);
        assertEquals("2025-10-25", sqlDate.toString());
    }

    @Test
    void testToSqlTimestamp() {
        // Arrange
        String dateTimeString = "2025-10-25T14:30:00.000Z";
        ZonedDateTime zonedDateTime = dateConfig.parseDateTime(dateTimeString);

        // Act
        java.sql.Timestamp sqlTimestamp = dateConfig.toSqlTimestamp(zonedDateTime);

        // Assert
        assertNotNull(sqlTimestamp);
        // Convert to UTC to verify the timestamp represents the correct instant
        java.time.Instant instant = zonedDateTime.toInstant();
        assertEquals(instant.toEpochMilli(), sqlTimestamp.getTime());
    }

    @Test
    void testToSqlDateWithNull() {
        // Act & Assert
        assertNull(dateConfig.toSqlDate(null));
    }

    @Test
    void testToSqlTimestampWithNull() {
        // Act & Assert
        assertNull(dateConfig.toSqlTimestamp(null));
    }

    @Test
    void testFormatDateTime() {
        // Arrange
        String originalDateTime = "2025-10-25T14:30:00.000Z";
        ZonedDateTime zonedDateTime = dateConfig.parseDateTime(originalDateTime);

        // Act
        String formatted = dateConfig.formatDateTime(zonedDateTime);

        // Assert
        assertNotNull(formatted);
        assertEquals(originalDateTime, formatted);
    }

    @Test
    void testFormatDateTimeWithNull() {
        // Act & Assert
        assertNull(dateConfig.formatDateTime(null));
    }

    @Test
    void testGetDatePattern() {
        // Act
        String pattern = dateConfig.getDatePattern();

        // Assert
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", pattern);
    }

    @Test
    void testGetDefaultZone() {
        // Act
        ZoneId defaultZone = dateConfig.getDefaultZone();

        // Assert
        assertEquals(ZoneId.of("UTC"), defaultZone);
    }

    @Test
    void testParseWithMilliseconds() {
        // Arrange
        String dateTimeString = "2025-10-25T14:30:45.123Z";

        // Act
        ZonedDateTime result = dateConfig.parseDateTime(dateTimeString);

        // Assert
        assertNotNull(result);
        assertEquals(45, result.getSecond());
        assertEquals(123000000, result.getNano()); // 123 milliseconds in nanoseconds
    }

    @Test
    void testTimezoneConversion() {
        // Arrange - Same moment in time with different timezones
        String utcTime = "2025-10-25T14:30:00.000Z";
        String istTime = "2025-10-25T20:00:00.000+05:30"; // Same moment as 14:30 UTC

        // Act
        ZonedDateTime utcDateTime = dateConfig.parseDateTime(utcTime);
        ZonedDateTime istDateTime = dateConfig.parseDateTime(istTime);

        // Assert - Both should represent the same instant
        assertEquals(utcDateTime.toInstant(), istDateTime.toInstant());
    }
}
