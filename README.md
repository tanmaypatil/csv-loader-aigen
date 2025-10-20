# CSV Database Importer

A simple, maintainable Java tool for importing CSV files into PostgreSQL database. Designed to be easy for junior developers to understand and modify.

## Features

- **Simple CSV Import**: Automatically imports CSV files into database tables
- **Table Name Mapping**: Table names automatically derived from CSV filenames (e.g., `employee.csv` → `employee` table)
- **Configurable Date Formats**: Customize date parsing format via configuration file
- **Type Detection**: Automatically handles different data types (integers, decimals, dates, strings)
- **SQL Injection Protection**: Uses parameterized queries for security
- **Database Abstraction**: Code designed to easily support other databases (Oracle) in the future
- **Comprehensive Tests**: Full unit test coverage using PostgreSQL

## Tech Stack

- **Language**: Java 17 (LTS)
- **Build Tool**: Maven
- **Database**: PostgreSQL (Oracle support planned)
- **Testing**: JUnit 5
- **Dependencies**: Minimal - PostgreSQL JDBC driver only

## Project Structure

```
csv-loader-aigen/
├── src/
│   ├── main/
│   │   ├── java/com/csvloader/
│   │   │   ├── CSVImporter.java          # Main import logic
│   │   │   ├── CSVImporterApp.java       # CLI application
│   │   │   ├── DatabaseConnection.java   # DB connection utility
│   │   │   └── DateFormatConfig.java     # Date parsing utility
│   │   └── resources/
│   │       ├── database.properties       # DB configuration
│   │       └── employee.csv              # Sample CSV file
│   └── test/
│       ├── java/com/csvloader/
│       │   ├── CSVImporterTest.java
│       │   └── DatabaseConnectionTest.java
│       └── resources/
│           └── test-database.properties  # Test DB config
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

Edit `src/main/resources/database.properties`:

```properties
# Database Connection
db.url=jdbc:postgresql://localhost:5432/yourdb
db.username=postgres
db.password=your_password
db.driver=org.postgresql.Driver

# Date format (Java SimpleDateFormat pattern)
date.format=yyyy-MM-dd

# CSV configuration
csv.delimiter=,
csv.has.header=true
```

## Building

Build the project:
```bash
mvn clean package
```

This creates `target/csv-database-importer-1.0.0.jar`

## Usage

### Running the Application

```bash
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

To change date format, update `database.properties`:

```properties
# For DD/MM/YYYY format
date.format=dd/MM/yyyy

# For MM-DD-YYYY format
date.format=MM-dd-yyyy
```

Supported formats follow Java's SimpleDateFormat patterns.

## Running Tests

### Test Prerequisites

1. Create test database:
   ```sql
   CREATE DATABASE testdb;
   ```

2. Update `src/test/resources/test-database.properties` with your test database credentials

### Run Tests

```bash
mvn test
```

Tests automatically:
- Create the `employee` table
- Run all test cases
- Clean up after completion

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

## Future Enhancements

- **Oracle Support**: Code is abstracted to easily add Oracle driver
- **Multiple Date Formats**: Support different formats per column
- **Batch Processing**: Import multiple CSV files at once
- **Progress Indicators**: Show import progress for large files
- **Validation Mode**: Dry-run to validate CSV before import

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

## Contributing

This codebase is designed for junior developers. Key principles:

- **Simple over clever**: Readable code preferred
- **Clear naming**: Variables and methods self-document
- **Comments**: Explain "why", not "what"
- **No complex libraries**: Use Java standard library when possible

## License

See LICENSE file for details.
