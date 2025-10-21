# CSV Database Importer

A simple, maintainable Java tool for importing CSV files into PostgreSQL database. Designed to be easy for junior developers to understand and modify.

## Features

- **Multiple File Import**: Import multiple CSV files in batch via configuration list
- **CSV Export**: Generate CSV template files from database table schemas with column headers only
- **Simple CSV Import**: Automatically imports CSV files into database tables
- **Table Name Mapping**: Table names automatically derived from CSV filenames (e.g., `employee.csv` → `employee` table)
- **Configurable Date Formats**: Customize date parsing format via configuration file
- **Type Detection**: Automatically handles different data types (integers, decimals, dates, strings)
- **SQL Injection Protection**: Uses parameterized queries for security
- **Database Abstraction**: Code designed to easily support other databases (Oracle) in the future
- **Comprehensive Logging**: SLF4J/Logback logging with file and console output
- **Separated Configuration**: Database and application settings in separate files
- **Comprehensive Tests**: Full unit test coverage using PostgreSQL (58+ tests)

## Tech Stack

- **Language**: Java 17 (LTS)
- **Build Tool**: Maven
- **Database**: PostgreSQL (Oracle support planned)
- **Testing**: JUnit 5
- **Logging**: SLF4J 2.0.9 + Logback 1.4.11
- **Dependencies**: Minimal - PostgreSQL JDBC driver, SLF4J/Logback

## Project Structure

```
csv-loader-aigen/
├── src/
│   ├── main/
│   │   ├── java/com/csvloader/
│   │   │   ├── ApplicationConfig.java     # Application settings loader
│   │   │   ├── CSVExporter.java           # CSV export logic
│   │   │   ├── CSVImporter.java           # Main import logic
│   │   │   ├── CSVImporterApp.java        # CLI application
│   │   │   ├── DatabaseConnection.java    # DB connection utility
│   │   │   └── DateFormatConfig.java      # Date parsing utility
│   │   └── resources/
│   │       ├── application.properties     # App configuration
│   │       ├── database.properties        # DB configuration
│   │       ├── logback.xml                # Logging configuration
│   │       ├── employee.csv               # Sample CSV file
│   │       └── exported-csv/              # Generated CSV templates
│   └── test/
│       ├── java/com/csvloader/
│       │   ├── ApplicationConfigTest.java      # 40 tests
│       │   ├── CSVExporterTest.java            # 10 tests
│       │   ├── CSVImporterTest.java            # 12 tests
│       │   └── DatabaseConnectionTest.java     # 7 tests
│       └── resources/
│           ├── test-application.properties     # Test app config
│           └── test-database.properties        # Test DB config
├── logs/                                   # Generated log files
├── pom.xml
└── README.md
```

## Setup

### Prerequisites

1. **Java 17 or higher**
   ```bash
   java -version
   ```

2. **Maven**
   ```bash
   mvn -version
   ```

3. **PostgreSQL** running locally or accessible remotely

### Database Setup

1. Create a database:
   ```sql
   CREATE DATABASE yourdb;
   ```

2. Create a table matching your CSV structure:
   ```sql
   CREATE TABLE employee (
       id INTEGER PRIMARY KEY,
       name VARCHAR(100) NOT NULL,
       email VARCHAR(100),
       department VARCHAR(50),
       hire_date DATE,
       salary NUMERIC(10, 2)
   );
   ```

### Configuration

The application uses two separate configuration files for better organization:

#### 1. Database Configuration (`src/main/resources/database.properties`)
```properties
# Database Connection Configuration
db.url=jdbc:postgresql://localhost:5432/yourdb
db.username=postgres
db.password=your_password
db.driver=org.postgresql.Driver
```

#### 2. Application Configuration (`src/main/resources/application.properties`)
```properties
# Date format (Java SimpleDateFormat pattern)
# Common patterns:
# yyyy-MM-dd (e.g., 2024-01-15)
# dd/MM/yyyy (e.g., 15/01/2024)
# MM-dd-yyyy (e.g., 01-15-2024)
date.format=yyyy-MM-dd

# CSV configuration
csv.delimiter=,
csv.has.header=true

# CSV files to import (comma-separated list)
# Files must be present in src/main/resources directory
# Table names will be derived from filenames (e.g., employee.csv -> employee table)
# Example: csv.files=employee.csv,department.csv,salary.csv
csv.files=employee.csv

# CSV Export configuration
# Enable export of table headers to CSV files
export.enabled=false

# Tables to export headers for (comma-separated list)
# CSV files will be generated with column names as first row only
# Example: export.tables=employee,department,projects
export.tables=

# Output directory for exported CSV files (relative to project root)
# Directory will be created if it doesn't exist
export.output.dir=src/main/resources/exported-csv
```

## Building

Build the project:
```bash
mvn clean package
```

This creates `target/csv-database-importer-1.0.0.jar`

## Usage

### Running the Application

The application supports two modes of operation:

#### Mode 1: Import All Configured Files
```bash
# Imports all files listed in application.properties (csv.files)
java -jar target/csv-database-importer-1.0.0.jar
```

**Output:**
```
Date format configured as: yyyy-MM-dd
Importing 3 file(s) from configuration:
  - employee.csv
  - department.csv
  - salary.csv

Importing: employee.csv
  ✓ Imported 5 rows

Importing: department.csv
  ✓ Imported 3 rows

Importing: salary.csv
  ✓ Imported 8 rows

========================================
Import Summary:
  Files processed: 3/3
  Total rows imported: 16
========================================
```

#### Mode 2: Import Specific File
```bash
# Imports only the specified file
java -jar target/csv-database-importer-1.0.0.jar employee.csv
```

### CSV File Format

1. Place CSV files in `src/main/resources/`
2. First row must contain column names matching database table columns
3. Subsequent rows contain data

Example (`employee.csv`):
```csv
id,name,email,department,hire_date,salary
1,John Doe,john.doe@example.com,Engineering,2023-01-15,75000.00
2,Jane Smith,jane.smith@example.com,Marketing,2023-02-20,68000.00
```

### Customizing Date Format

To change date format, update `application.properties`:

```properties
# For DD/MM/YYYY format
date.format=dd/MM/yyyy

# For MM-DD-YYYY format
date.format=MM-dd-yyyy
```

Supported formats follow Java's SimpleDateFormat patterns.

### Multiple File Import

To import multiple CSV files in batch, configure them in `application.properties`:

```properties
# Comma-separated list of CSV files
csv.files=employee.csv,department.csv,salary.csv,benefits.csv
```

Then run without arguments:
```bash
java -jar target/csv-database-importer-1.0.0.jar
```

All files will be imported sequentially with a summary report at the end.

### CSV Export (Generate Templates from Database Tables)

The application can export database table schemas to CSV files containing only column headers. This is useful for:
- Creating CSV templates for data entry
- Documenting table structures
- Preparing files for external data population

#### Enable Export

Update `application.properties`:

```properties
# Enable export feature
export.enabled=true

# Specify tables to export (comma-separated)
export.tables=employee,department,projects

# Output directory (will be created if it doesn't exist)
export.output.dir=src/main/resources/exported-csv
```

#### Run Export

When export is enabled, the application will automatically export table headers after completing any import operations:

```bash
java -jar target/csv-database-importer-1.0.0.jar
```

**Output:**
```
========================================
CSV Export:
========================================
Exporting headers for 3 table(s):
  - employee
  - department
  - projects
Output directory: src/main/resources/exported-csv

Export Summary:
  Tables exported: 3/3
  Output location: src/main/resources/exported-csv
========================================
```

#### Generated CSV Files

Each table generates a CSV file with only the column headers:

**employee.csv:**
```csv
id,name,email,department,hire_date,salary
```

**department.csv:**
```csv
dept_id,dept_name,location
```

**projects.csv:**
```csv
project_id,project_name,start_date,budget
```

You can then populate these files with data and import them back into the database.

#### Export Features

- **Automatic directory creation**: Output directory is created if it doesn't exist
- **Column order preservation**: Columns exported in database table order
- **Configurable delimiter**: Uses the same `csv.delimiter` setting as import
- **Error handling**: Invalid tables are logged and skipped, valid tables still export
- **Batch processing**: Export multiple tables in one operation

## Logging

The application uses SLF4J with Logback for comprehensive logging.

### Log Configuration

Logging is configured in `src/main/resources/logback.xml`:
- **Console Output**: Shows INFO level and above
- **File Output**: `logs/csv-importer.log` (DEBUG level)
- **Daily Rotation**: Log files rotate daily, keeping 30 days of history

### Log Levels

```xml
<!-- Application logs (DEBUG level) -->
<logger name="com.csvloader" level="DEBUG" />

<!-- Third-party libraries (WARN level) -->
<root level="WARN" />
```

### Viewing Logs

**Console output** during execution:
```
2025-10-20 23:30:15.123 [main] INFO  com.csvloader.CSVImporterApp - ========== CSV Database Importer Starting ==========
2025-10-20 23:30:15.234 [main] INFO  com.csvloader.DatabaseConnection - Database connection established successfully
2025-10-20 23:30:15.345 [main] INFO  com.csvloader.CSVImporter - Date format configured as: yyyy-MM-dd
2025-10-20 23:30:15.456 [main] INFO  com.csvloader.CSVImporterApp - Starting import of file: employee.csv
2025-10-20 23:30:15.567 [main] INFO  com.csvloader.CSVImporterApp - Successfully imported 5 rows from employee.csv
```

**Log file** (`logs/csv-importer.log`):
```bash
tail -f logs/csv-importer.log
```

### Customizing Logging

To change log level, edit `logback.xml`:
```xml
<!-- More verbose logging (DEBUG) -->
<logger name="com.csvloader" level="DEBUG" />

<!-- Less verbose logging (INFO) -->
<logger name="com.csvloader" level="INFO" />

<!-- Minimal logging (WARN) -->
<logger name="com.csvloader" level="WARN" />
```

## Running Tests

### Test Prerequisites

1. Create test database:
   ```sql
   CREATE DATABASE testdb;
   ```

2. Update `src/test/resources/test-database.properties` with your test database credentials

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApplicationConfigTest

# Run with detailed output
mvn test -X
```

**Test Coverage:**
- **ApplicationConfigTest**: 40 tests for configuration loading and parsing (including export config)
- **DatabaseConnectionTest**: 7 tests for database connectivity
- **CSVImporterTest**: 12 tests for CSV import functionality
- **CSVExporterTest**: 10 tests for CSV export functionality
- **Total**: 69+ comprehensive tests

Tests automatically:
- Create the `employee` table
- Run all test cases with detailed logging
- Clean up after completion
- Generate logs in `logs/test-execution.log`

## Supported Data Types

The importer automatically handles:

- **Integers**: `int`, `integer`, `serial`, `bigint`
- **Decimals**: `numeric`, `decimal`, `real`, `float`, `double`
- **Dates**: `date`, `timestamp` (using configured format)
- **Booleans**: `boolean`
- **Strings**: `varchar`, `text`, `char`

## Error Handling

The application provides clear error messages for:

- Missing or invalid CSV files
- Database connection failures
- Column count mismatches
- Invalid date formats
- Invalid number formats
- Missing database tables or columns

## Recent Updates

### Version 1.0.0 Features:
- ✅ **CSV Export**: Generate CSV template files from database table schemas
- ✅ **Multiple File Import**: Batch import via comma-separated configuration
- ✅ **Comprehensive Logging**: SLF4J/Logback with file and console output
- ✅ **Separated Configuration**: Database and application settings in separate files
- ✅ **69+ Unit Tests**: Full test coverage for all components including export
- ✅ **Custom Agents**: `java-unit-test-generator` agent for automated test generation

## Future Enhancements

- **Oracle Support**: Code is abstracted to easily add Oracle driver
- **Multiple Date Formats**: Support different formats per column
- **Progress Indicators**: Show import progress for large files
- **Validation Mode**: Dry-run to validate CSV before import
- **Transaction Support**: Rollback on failure for multi-file imports

## Adding Oracle Support

To add Oracle support in the future:

1. Add Oracle JDBC driver to `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.oracle.database.jdbc</groupId>
       <artifactId>ojdbc8</artifactId>
       <version>21.5.0.0</version>
   </dependency>
   ```

2. Update `database.properties`:
   ```properties
   db.url=jdbc:oracle:thin:@localhost:1521:orcl
   db.driver=oracle.jdbc.driver.OracleDriver
   ```

3. Modify `getColumnTypes()` in `CSVImporter.java` to handle Oracle's metadata queries

## Troubleshooting

### Connection Failed
- Verify PostgreSQL is running
- Check `database.properties` credentials
- Ensure database exists

### Date Parse Error
- Check date format in CSV matches `date.format` in properties
- Verify dates are in consistent format

### Column Not Found
- Ensure CSV headers match database column names exactly
- Column names are case-sensitive

## Quick Reference

### Common Commands
```bash
# Build project
mvn clean package

# Run with all configured files
java -jar target/csv-database-importer-1.0.0.jar

# Run with specific file
java -jar target/csv-database-importer-1.0.0.jar employee.csv

# Run tests
mvn test

# View logs
tail -f logs/csv-importer.log
```

### File Locations
- **Database Config**: `src/main/resources/database.properties`
- **App Config**: `src/main/resources/application.properties`
- **CSV Files**: `src/main/resources/*.csv`
- **Logs**: `logs/csv-importer.log`
- **Tests**: `src/test/java/com/csvloader/`

### Configuration Keys
| Key | Location | Description | Default |
|-----|----------|-------------|---------|
| `db.url` | database.properties | Database JDBC URL | - |
| `db.username` | database.properties | Database username | - |
| `db.password` | database.properties | Database password | - |
| `date.format` | application.properties | Date format pattern | yyyy-MM-dd |
| `csv.delimiter` | application.properties | CSV delimiter | , |
| `csv.files` | application.properties | Files to import | employee.csv |
| `export.enabled` | application.properties | Enable CSV export | false |
| `export.tables` | application.properties | Tables to export | (empty) |
| `export.output.dir` | application.properties | Export output directory | src/main/resources/exported-csv |

## Contributing

This codebase is designed for junior developers. Key principles:

- **Simple over clever**: Readable code preferred
- **Clear naming**: Variables and methods self-document
- **Comments**: Explain "why", not "what"
- **No complex libraries**: Use Java standard library when possible
- **Comprehensive logging**: Use SLF4J for all logging
- **Full test coverage**: Write tests for all new features

## License

See LICENSE file for details.
