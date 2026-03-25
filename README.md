# Java Spring Boot SQL2O REST CRUD

A RESTful API application built with Spring Boot, SQL2O, and MySQL for managing products, companies, and tutorials.

## Tech Stack

| Technology        | Version   | Purpose               |
| ----------------- | --------- | --------------------- |
| Java              | 21        | Runtime               |
| Spring Boot       | 4.0.4     | Framework             |
| SQL2O             | 1.9.1     | Database access layer |
| ElSql             | 1.3       | SQL management        |
| MySQL Connector/J | 9.6.0     | MySQL driver          |
| Lombok            | (managed) | Boilerplate reduction |
| Jackson           | (managed) | JSON serialization    |
| Log4j2            | (managed) | Logging               |

## Project Structure

```text
src/main/java/com/otis/
├── Application.java                    # Main entry point
├── config/
│   └── DatabaseConfig.java            # DataSource & Sql2o configuration
├── controller/
│   ├── ProductController.java         # Product REST endpoints
│   └── TutorialController.java        # Tutorial REST endpoints
├── exception/
│   ├── ControllerExceptionHandler.java # Global exception handling
│   ├── ErrorMessage.java             # Error response model
│   └── ResourceNotFoundException.java # Custom 404 exception
├── model/
│   ├── Company.java                   # Company entity
│   ├── Product.java                  # Product entity
│   ├── Response.java                 # Report response wrapper
│   ├── Tutorial.java                 # Tutorial entity
│   └── TutorialDetails.java          # Tutorial details entity
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
├── application.properties              # Application configuration
├── log4j2.xml                          # Log4j2 configuration
└── com/otis/repository/
    ├── CompanyRepository.elsql          # Company SQL queries
    ├── ProductRepository.elsql         # Product SQL queries
    └── TutorialRepository.elsql        # Tutorial SQL queries

db/
└── java-spring-boot-sql2o-rest-crud.sql # Database schema & sample data
```

## API Endpoints

### Products

| Method | Endpoint                                | Description                              |
| ------ | --------------------------------------- | ---------------------------------------- |
| GET    | `/api/products`                         | Get all products (optional: `?name=xxx`) |
| GET    | `/api/products/company?companyName=xxx` | Get products by company name             |
| GET    | `/api/products/report`                  | Get product-company report data          |
| GET    | `/api/products/{id}`                    | Get product by ID                        |

### Tutorials

| Method | Endpoint              | Description                                |
| ------ | --------------------- | ------------------------------------------ |
| GET    | `/api/tutorials`      | Get all tutorials (optional: `?title=xxx`) |
| GET    | `/api/tutorials/{id}` | Get tutorial by ID                         |

## Configuration

Configure database connection in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/java-spring-boot-sql2o-rest-crud
spring.datasource.username=root
spring.datasource.password=your_password
server.port=6661
```

## Database Setup

1. Create the database:

```sql
CREATE DATABASE java-spring-boot-sql2o-rest-crud;
```

1. Run the schema script:

```bash
mysql -u root -p java-spring-boot-sql2o-rest-crud < db/java-spring-boot-sql2o-rest-crud.sql
```

## Build & Run

### Using Makefile (Recommended)

```bash
make clean    # Clean build artifacts
make build    # Build package (skip tests)
make run      # Build and run application
make test     # Run tests
```

### Using Maven

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/java-spring-boot-sql2o-rest-crud-1.0-SNAPSHOT.jar
```

## Key Features

- **SQL2O**: Lightweight JDBC wrapper for easy database operations
- **ElSql**: External SQL file management for clean repository code
- **CORS Enabled**: Cross-origin requests allowed for all origins
- **Global Exception Handling**: Consistent error responses via `@RestControllerAdvice`
- **Rolling File Logging**: Daily rotating logs with size limits

## Response Format

### Success Response

```json
{
  "id": 1,
  "name": "Product Name",
  ...
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
