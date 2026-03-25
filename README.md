# Java Spring Boot Sql2o REST CRUD

A RESTful API application built with Spring Boot 4, Sql2o, Flyway, and MySQL for managing products, companies, and tutorials.

## Tech Stack

| Technology        | Version   | Purpose                   |
| ----------------- | --------- | ------------------------- |
| Java              | 21        | Runtime                   |
| Spring Boot       | 4.0.4     | Framework                 |
| Sql2o             | 1.9.1     | Database access layer     |
| ElSql             | 1.3       | SQL management            |
| MySQL Connector/J | 9.6.0     | MySQL driver              |
| Flyway            | (managed) | Database migration        |
| HikariCP          | (managed) | Connection pooling        |
| Lombok            | (managed) | Boilerplate reduction     |
| Jackson           | (managed) | JSON serialization        |
| Logback           | (managed) | JSON logging with traceId |

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
├── logback-spring.xml                    # Logback JSON configuration
├── META-INF/
│   └── additional-spring-configuration-metadata.json  # Spring config metadata
├── db/migration/
│   ├── V20260325130814__create_table_company.sql       # Company table
│   ├── V20260325130854__create_table_products.sql      # Products table
│   ├── V20260325130933__create_table_products_company.sql # Product-Company relation
│   ├── V20260325131006__create_table_tutorials.sql      # Tutorials table
│   └── V20260325131038__create_table_tutorial_details.sql # Tutorial details table
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
# Development
make help            # Show available commands
make clean          # Clean build artifacts
make build          # Build package (skip tests)
make run            # Build and run application
make test           # Run tests

# Database Management
make gen-migration desc=your_description  # Generate new Flyway migration
make db-migrate     # Run Flyway migrations
make db-info        # Show migration status
make db-repair      # Repair Flyway checksum mismatches
make db-clean       # DROP all database objects (dev only, requires confirmation)
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

| Variable              | Default                          | Description        |
| --------------------- | -------------------------------- | ------------------ |
| DB_HOST               | localhost                        | Database host      |
| DB_PORT               | 3306                             | Database port      |
| DB_NAME               | java-spring-boot-sql2o-rest-crud | Database name      |
| DB_USERNAME           | root                             | Database username  |
| DB_PASSWORD           | root                             | Database password  |
| LOG_PATH              | logs                             | Log directory path |
| MYSQL_DOCKER_HOST     | localhost                        | MySQL Docker host  |
| MYSQL_DOCKER_PORT     | 3306                             | MySQL Docker port  |
| MYSQL_DOCKER_DATABASE | java-spring-boot-sql2o-rest-crud | Database name      |
| MYSQL_DOCKER_USERNAME | root                             | Database username  |
| MYSQL_DOCKER_PASSWORD |                                  | Database password  |

## Key Features

- **UUID v7**: Time-ordered unique identifiers for all entities
- **HikariCP**: High-performance connection pooling
- **Flyway**: Version-controlled database migrations
- **Sql2o**: Lightweight JDBC wrapper for easy database operations
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

## Log Output Format

### Request Start

```json
{"timestamp":"2026-03-25T19:17:48.485351932+07:00","level":"INFO","thread_name":"http-nio-6661-exec-2","message":"Request started","caller":{"class":"com.otis.config.TraceIdFilter","method":"doFilterInternal","file":"TraceIdFilter.java","line":49},"traceId":"19d24edf7c5841abf","method":"GET","event":"START","uri":"/api/products"}
```

### Request End

```json
{"timestamp":"2026-03-25T19:17:48.841024008+07:00","level":"INFO","thread_name":"http-nio-6661-exec-2","message":"Request completed","caller":{"class":"com.otis.config.TraceIdFilter","method":"doFilterInternal","file":"TraceIdFilter.java","line":61},"traceId":"19d24edf7c5841abf","method":"GET","event":"END","uri":"/api/products","processTime":"355 ms","status":"200"}
```

### Database Query

```json
{"timestamp":"2026-03-25T19:17:48.523154262+07:00","level":"INFO","thread_name":"http-nio-6661-exec-2","message":"GetAllProduct: SELECT id, name, company_id FROM products ","caller":{"class":"com.otis.repository.ProductRepository","method":"findAll","file":"ProductRepository.java","line":34},"traceId":"19d24edf7c5841abf","method":"GET","event":"START","uri":"/api/products"}
```

## License

MIT
