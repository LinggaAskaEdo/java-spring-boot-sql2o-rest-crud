# Java Spring Boot SQL2O REST CRUD

A RESTful API application built with Spring Boot 4, SQL2O, Flyway, and MySQL for managing products, companies, and tutorials.

## Tech Stack

| Technology        | Version   | Purpose                   |
| ----------------- | --------- | ------------------------- |
| Java              | 21        | Runtime                   |
| Spring Boot       | 4.0.4     | Framework                 |
| SQL2O             | 1.9.1     | Database access layer     |
| ElSql             | 1.3       | SQL management            |
| MySQL Connector/J | 9.6.0     | MySQL driver              |
| Flyway            | (managed) | Database migration        |
| HikariCP          | (managed) | Connection pooling        |
| Lombok            | (managed) | Boilerplate reduction     |
| Jackson           | (managed) | JSON serialization        |
| Log4j2            | (managed) | JSON logging with traceId |

## Project Structure

```text
src/main/java/com/otis/
├── Application.java                    # Main entry point
├── config/
│   ├── DatabaseConfig.java            # Sql2o configuration
│   └── TraceIdFilter.java            # Request tracing filter
├── controller/
│   ├── ProductController.java         # Product REST endpoints
│   └── TutorialController.java        # Tutorial REST endpoints
├── exception/
│   ├── ControllerExceptionHandler.java # Global exception handling
│   ├── ErrorMessage.java             # Error response model
│   └── ResourceNotFoundException.java # Custom 404 exception
├── model/
│   ├── Company.java                   # Company entity (UUID v7)
│   ├── Product.java                  # Product entity (UUID v7)
│   ├── Response.java                 # Report response wrapper
│   ├── Tutorial.java                 # Tutorial entity (UUID v7)
│   └── TutorialDetails.java          # Tutorial details entity (UUID v7)
├── preference/
│   └── ConstantPreference.java        # Column mapping constants
├── repository/
│   ├── CompanyRepository.java         # Company data access
│   ├── ProductRepository.java         # Product data access
│   └── TutorialRepository.java        # Tutorial data access
└── service/
    ├── ProductService.java            # Product business logic
    └── TutorialService.java           # Tutorial business logic

src/main/resources/
├── application.yaml                    # Application configuration
├── log4j2.xml                       # Log4j2 JSON configuration
├── db/migration/
│   └── V1__initial_schema.sql        # Flyway migration with UUID v7
└── com/otis/repository/
    ├── CompanyRepository.elsql         # Company SQL queries
    ├── ProductRepository.elsql        # Product SQL queries
    └── TutorialRepository.elsql       # Tutorial SQL queries
```

## API Endpoints

### Products

| Method | Endpoint                                | Description                              |
| ------ | --------------------------------------- | ---------------------------------------- |
| GET    | `/api/products`                         | Get all products (optional: `?name=xxx`) |
| GET    | `/api/products/{id}`                    | Get product by UUID                      |
| GET    | `/api/products/company?companyName=xxx` | Get products by company name             |
| GET    | `/api/products/report`                  | Get product-company report data          |

### Tutorials

| Method | Endpoint              | Description                                |
| ------ | --------------------- | ------------------------------------------ |
| GET    | `/api/tutorials`      | Get all tutorials (optional: `?title=xxx`) |
| GET    | `/api/tutorials/{id}` | Get tutorial by UUID                       |

## Configuration

Configure via environment variables or `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:java-spring-boot-sql2o-rest-crud}?useSSL=false
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    hikari:
      pool-name: HikariPool-1
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

## Database Setup

### Using Makefile (Recommended)

```bash
# Create database and run migrations
make db-create     # Create database
make db-migrate    # Run Flyway migrations

# Or reset everything
make db-reset      # Drop, create, migrate
```

### Manual Setup

```bash
# 1. Create database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS java-spring-boot-sql2o-rest-crud;"

# 2. Run with Flyway auto-migration
mvn spring-boot:run
```

## Build & Run

### Using Makefile

```bash
make clean        # Clean build artifacts
make build        # Build package (skip tests)
make run          # Build and run application
make test         # Run tests
make db-status    # Check migration status
```

### Using Maven

```bash
# Build
mvn clean package -DskipTests

# Run (Flyway auto-migrates on startup)
mvn spring-boot:run

# Or run the JAR
java -jar target/java-spring-boot-sql2o-rest-crud-1.0-SNAPSHOT.jar
```

## Environment Variables

| Variable    | Default                          | Description        |
| ----------- | -------------------------------- | ------------------ |
| DB_HOST     | localhost                        | Database host      |
| DB_PORT     | 3306                             | Database port      |
| DB_NAME     | java-spring-boot-sql2o-rest-crud | Database name      |
| DB_USERNAME | root                             | Database username  |
| DB_PASSWORD | root                             | Database password  |
| LOG_PATH    | logs                             | Log directory path |

## Key Features

- **UUID v7**: Time-ordered unique identifiers for all entities
- **HikariCP**: High-performance connection pooling
- **Flyway**: Version-controlled database migrations
- **SQL2O**: Lightweight JDBC wrapper for easy database operations
- **ElSql**: External SQL file management for clean repository code
- **JSON Logging**: Structured JSON logs with traceId support
- **Request Tracing**: X-Trace-Id header for distributed tracing
- **CORS Enabled**: Cross-origin requests allowed for all origins
- **Global Exception Handling**: Consistent error responses

## Response Format

### Success Response

```json
{
  "id": "018e0000-0000-7000-8000-000000000001",
  "name": "LVIV"
}
```

### Error Response

```json
{
  "statusCode": 404,
  "timestamp": "2026-03-25T10:30:00",
  "message": "Resource not found",
  "description": "/api/products/999"
}
```

## License

MIT
