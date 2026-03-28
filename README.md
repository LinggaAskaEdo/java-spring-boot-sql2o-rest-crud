# Java Spring Boot Sql2o REST CRUD

A RESTful API application built with Spring Boot 4, Sql2o, Flyway, and MySQL for managing products, companies, tutorials, and seat reservations.

## Tech Stack

| Technology        | Version   | Purpose                                    |
| ----------------- | --------- | ------------------------------------------ |
| Java              | 21        | Runtime                                    |
| Spring Boot       | 4.0.5     | Framework                                  |
| Redisson          | 4.3.0     | Distributed rate limiting with Redis       |
| Resilience4j      | 2.4.0     | Bulkhead for concurrency control           |
| Sql2o             | 1.9.1     | Database access layer                      |
| ElSql             | 1.3       | SQL management                             |
| MySQL Connector/J | 9.6.0     | MySQL driver                               |
| Flyway            | (managed) | Database migration                         |
| HikariCP          | (managed) | Connection pooling                         |
| Lombok            | (managed) | Boilerplate reduction                      |
| Jackson           | (managed) | JSON serialization                         |
| Logback           | (managed) | JSON logging with traceId                  |
| Virtual Threads   | Java 21   | Lightweight threading for high scalability |

## Project Structure

```text
src/main/java/com/otis/
├── Application.java                    # Main entry point
├── config/
│   ├── DatabaseConfig.java           # Sql2o configuration
│   ├── BulkheadConfig.java            # Resilience4j bulkhead configuration
│   ├── LoggingResponseWrapper.java    # Response body capture wrapper
│   ├── TraceIdFilter.java             # Request tracing filter
│   ├── VirtualThreadConfig.java        # Jetty virtual thread configuration
│   ├── RedissonConfig.java            # Redisson client configuration
│   ├── RateLimiterProperties.java     # Rate limiter configuration properties
│   ├── RateLimiterKeyInitializer.java # Startup rate limiter key initialization
│   └── RateLimiterFilter.java        # API key validation & rate limiting filter
├── controller/
│   ├── CompanyController.java         # Company REST endpoints
│   ├── EventController.java           # Event REST endpoints
│   ├── ProductController.java         # Product REST endpoints
│   ├── SeatController.java            # Seat & Reservation REST endpoints
│   └── TutorialController.java        # Tutorial REST endpoints
├── exception/
│   ├── BadRequestException.java       # Custom 400 exception
│   ├── BulkheadFullException.java     # Bulkhead full exception
│   ├── UnauthorizedException.java     # Unauthorized exception (401)
│   ├── RateLimitExceededException.java # Rate limit exceeded exception (429)
│   ├── ControllerExceptionHandler.java # Global exception handling
│   ├── ErrorMessage.java              # Error response model
│   └── ResourceNotFoundException.java # Custom 404 exception
├── model/
│   ├── dto/
│   │   ├── ReservationRequest.java    # Reservation request DTO
│   │   ├── Response.java             # Report response wrapper
│   │   └── SeatAvailability.java     # Seat availability DTO
│   └── entity/
│       ├── Company.java               # Company entity
│       ├── Event.java                 # Event entity
│       ├── PageResponse.java          # Pagination response wrapper
│       ├── Product.java               # Product entity
│       ├── Reservation.java           # Reservation entity
│       ├── Seat.java                  # Seat entity
│       ├── Tutorial.java              # Tutorial entity
│       └── TutorialDetails.java       # Tutorial details entity
├── preference/
│   └── ConstantPreference.java        # Column mapping constants
├── repository/
│   ├── CompanyRepository.java         # Company data access
│   ├── EventRepository.java           # Event data access
│   ├── ProductRepository.java         # Product data access
│   ├── ReservationRepository.java     # Reservation data access
│   ├── SeatRepository.java            # Seat data access
│   └── TutorialRepository.java        # Tutorial data access
├── scheduler/
│   ├── DataSeederScheduler.java       # Data seeding scheduler
│   └── SeatSeederScheduler.java       # Seat test data seeder
├── util/
│   ├── BulkheadUtils.java            # Bulkhead helper utility
│   ├── JsonUtils.java                # ObjectMapper utility
│   ├── RandomUtils.java              # Random data generator utility
│   └── UuidUtils.java                # UUID parsing utility
└── service/
    ├── CompanyService.java            # Company business logic
    ├── EventService.java              # Event business logic
    ├── ProductService.java            # Product business logic
    ├── SeatService.java               # Seat business logic
    └── TutorialService.java           # Tutorial business logic

src/main/resources/
├── application.yaml                   # Application configuration
├── logback-spring.xml                 # Logback JSON configuration
├── META-INF/
│   └── additional-spring-configuration-metadata.json  # Spring config metadata
├── db/migration/
│   ├── V20260325130814__create_table_company.sql       # Company table
│   ├── V20260325130854__create_table_products.sql      # Products table
│   ├── V20260325130933__create_table_products_company.sql # Product-Company relation
│   ├── V20260325131006__create_table_tutorials.sql      # Tutorials table
│   ├── V20260325131038__create_table_tutorial_details.sql # Tutorial details table
│   ├── V20260327170000__create_table_events.sql         # Events table
│   ├── V20260327170001__create_table_seats.sql          # Seats table
│   └── V20260327170002__create_table_reservations.sql  # Reservations table
└── com/otis/
    └── repository/
        ├── CompanyRepository.elsql         # Company SQL queries
        ├── EventRepository.elsql           # Event SQL queries
        ├── ProductRepository.elsql        # Product SQL queries
        ├── ReservationRepository.elsql     # Reservation SQL queries
        ├── SeatRepository.elsql            # Seat SQL queries
        └── TutorialRepository.elsql        # Tutorial SQL queries
```

## API Endpoints

**Authentication Required**: All API endpoints require the `x-api-key` header. Requests without a valid API key will receive `401 Unauthorized`.

```bash
curl -H "x-api-key: vvip-key-001" http://localhost:6661/api/products
```

### Rate Limiting

Rate limits are enforced per tier based on the API key:

| Tier    | Max Requests/sec | Example API Keys                 |
| ------- | ---------------- | -------------------------------- |
| vvip    | 100              | vvip-key-001, vvip-key-002       |
| vip     | 50               | vip-key-001, vip-key-002         |
| premium | 10               | premium-key-001, premium-key-002 |
| general | 1                | general-key-001, general-key-002 |

Exceeding the rate limit returns `429 Too Many Requests`.

### Products

| Method | Endpoint        | Description                                  |
| ------ | --------------- | -------------------------------------------- |
| GET    | `/api/products` | Get all products with pagination and filters |

**Query Parameters:**

| Parameter     | Type   | Required | Default | Description                            |
| ------------- | ------ | -------- | ------- | -------------------------------------- |
| `page`        | int    | No       | 0       | Page number (0-indexed)                |
| `size`        | int    | No       | 10      | Page size                              |
| `id`          | UUID   | No       | -       | Filter by product ID                   |
| `name`        | String | No       | -       | Filter by name (partial match)         |
| `company`     | UUID   | No       | -       | Filter by company ID                   |
| `companyName` | String | No       | -       | Filter by company name (partial match) |

**Example:**

```bash
GET /api/products
GET /api/products?page=0&size=10
GET /api/products?name=Security
GET /api/products?name=Security&companyName=Singapore
```

### Companies

| Method | Endpoint         | Description                                   |
| ------ | ---------------- | --------------------------------------------- |
| GET    | `/api/companies` | Get all companies with pagination and filters |

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                    |
| --------- | ------ | -------- | ------- | ------------------------------ |
| `page`    | int    | No       | 0       | Page number (0-indexed)        |
| `size`    | int    | No       | 10      | Page size                      |
| `id`      | UUID   | No       | -       | Filter by company ID           |
| `name`    | String | No       | -       | Filter by name (partial match) |

**Example:**

```bash
GET /api/companies
GET /api/companies?page=0&size=5
GET /api/companies?name=Singapore
```

### Tutorials

| Method | Endpoint         | Description                                   |
| ------ | ---------------- | --------------------------------------------- |
| GET    | `/api/tutorials` | Get all tutorials with pagination and filters |

**Query Parameters:**

| Parameter     | Type    | Required | Default | Description                           |
| ------------- | ------- | -------- | ------- | ------------------------------------- |
| `page`        | int     | No       | 0       | Page number (0-indexed)               |
| `size`        | int     | No       | 10      | Page size                             |
| `id`          | UUID    | No       | -       | Filter by tutorial ID                 |
| `title`       | String  | No       | -       | Filter by title (partial match)       |
| `description` | String  | No       | -       | Filter by description (partial match) |
| `published`   | Boolean | No       | -       | Filter by published status            |

**Example:**

```bash
GET /api/tutorials
GET /api/tutorials?page=1&size=5
GET /api/tutorials?title=spring&published=true
```

### Events

| Method | Endpoint                           | Description                            |
| ------ | ---------------------------------- | -------------------------------------- |
| GET    | `/api/events`                      | Get all events with pagination/filters |
| GET    | `/api/events/{id}`                 | Get event by ID                        |
| GET    | `/api/events/{id}/seats/available` | Get available seats count              |

**Query Parameters (GET /api/events):**

| Parameter | Type   | Required | Default | Description                     |
| --------- | ------ | -------- | ------- | ------------------------------- |
| `page`    | int    | No       | 0       | Page number (0-indexed)         |
| `size`    | int    | No       | 10      | Page size                       |
| `id`      | UUID   | No       | -       | Filter by event ID              |
| `name`    | String | No       | -       | Filter by name (partial match)  |
| `venue`   | String | No       | -       | Filter by venue (partial match) |

**Example:**

```bash
GET /api/events
GET /api/events/019d2d72-eee3-7b29-9af2-f15d04e4b6d8
GET /api/events/019d2d72-eee3-7b29-9af2-f15d04e4b6d8/seats/available
```

### Seats & Reservations

| Method | Endpoint                                   | Description            |
| ------ | ------------------------------------------ | ---------------------- |
| GET    | `/api/events/{eventId}/seats`              | Get seats for an event |
| POST   | `/api/events/{eventId}/reserve`            | Reserve seats          |
| POST   | `/api/reservations/{reservationId}/cancel` | Cancel reservation     |

**Query Parameters (GET /api/events/{eventId}/seats):**

| Parameter | Type | Required | Default | Description             |
| --------- | ---- | -------- | ------- | ----------------------- |
| `page`    | int  | No       | 0       | Page number (0-indexed) |
| `size`    | int  | No       | 20      | Page size               |

**Reserve Seats Request:**

```bash
POST /api/events/{eventId}/reserve
Content-Type: application/json

{
  "customerName": "John Doe",
  "seatCount": 3
}
```

**Example:**

```bash
# Get seats for an event
GET /api/events/019d2d72-eee3-7b29-9af2-f15d04e4b6d8/seats
GET /api/events/019d2d72-eee3-7b29-9af2-f15d04e4b6d8/seats?page=0&size=50

# Reserve seats
curl -X POST http://localhost:6661/api/events/019d2d72-eee3-7b29-9af2-f15d04e4b6d8/reserve \
  -H "Content-Type: application/json" \
  -d '{"customerName": "John Doe", "seatCount": 3}'

# Cancel reservation
curl -X POST http://localhost:6661/api/reservations/019d2d72-eee3-7b29-9af2-f15d04e4b6d8/cancel
```

## Seat Reservation System

The seat reservation system uses **pessimistic locking** with `SELECT FOR UPDATE SKIP LOCKED` to prevent deadlocks and handle concurrent reservations.

### How It Works

1. **Find Available Seats**: Uses `FOR UPDATE SKIP LOCKED` to lock available seats atomically
2. **Reserve Seats**: Updates seat status within the same transaction
3. **Cancel Reservation**: Releases reserved seats back to available pool

### Concurrency Handling

- **SKIP LOCKED**: Prevents blocking when seats are already locked by another transaction
- **Transaction**: All seat updates in a single transaction for atomicity
- **Rollback**: Automatic rollback on failure

## Configuration

Configure via environment variables or `src/main/resources/application.yaml`:

```yaml
server:
  port: 6661

spring:
  threads:
    virtual:
      enabled: true # Enable Java 21 virtual threads
      name-prefix: ${VT_NAME_PREFIX:vt-jetty-} # Virtual thread name prefix

  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:java-spring-boot-sql2o-rest-crud}?useSSL=false
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    hikari:
      pool-name: HikariPool-1
      maximum-pool-size: 30
      throttle-percentage: 98 # Semaphore permits = 98% of max pool size
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

redisson:
  single-server-config:
    address: "redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}"
    password: ${REDIS_PASSWORD:}
    connection-minimum-idle-size: 1
    connection-pool-size: 10

rate-limiter:
  tiers:
    vvip:
      max-requests: ${RATE_LIMITER_VVIP_MAX_REQUESTS:100}
    vip:
      max-requests: ${RATE_LIMITER_VIP_MAX_REQUESTS:50}
    premium:
      max-requests: ${RATE_LIMITER_PREMIUM_MAX_REQUESTS:10}
    general:
      max-requests: ${RATE_LIMITER_GENERAL_MAX_REQUESTS:1}
  api-keys:
    vvip-key-001: vvip
    vvip-key-002: vvip
    vip-key-001: vip
    vip-key-002: vip
    premium-key-001: premium
    premium-key-002: premium
    general-key-001: general
    general-key-002: general

resilience4j:
  bulkhead:
    instances:
      database:
        max-concurrent-calls: ${BULKHEAD_MAX_CONCURRENT_CALLS:29}
        max-wait-duration: ${BULKHEAD_MAX_WAIT_DURATION:100ms}

scheduler:
  data-seeder:
    enabled: ${DATA_SEEDER_ENABLED:false}
    cron: ${DATA_SEEDER_CRON:0 0 0 * * ?}
    total-companies: ${DATA_SEEDER_TOTAL_COMPANIES:10}
    total-products: ${DATA_SEEDER_TOTAL_PRODUCTS:15}
    total-tutorials: ${DATA_SEEDER_TOTAL_TUTORIALS:10}
    max-products-per-company: ${DATA_SEEDER_MAX_PRODUCTS_PER_COMPANY:5}
    max-companies-per-product: ${DATA_SEEDER_MAX_COMPANIES_PER_PRODUCT:3}
  seat-seeder:
    enabled: ${SEAT_SEEDER_ENABLED:true}
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

| Variable                              | Default                          | Description                   |
| ------------------------------------- | -------------------------------- | ----------------------------- |
| DB_HOST                               | localhost                        | Database host                 |
| DB_PORT                               | 3306                             | Database port                 |
| DB_NAME                               | java-spring-boot-sql2o-rest-crud | Database name                 |
| DB_USERNAME                           | root                             | Database username             |
| DB_PASSWORD                           | root                             | Database password             |
| LOG_PATH                              | logs                             | Log directory path            |
| MYSQL_DOCKER_HOST                     | localhost                        | MySQL Docker host             |
| MYSQL_DOCKER_PORT                     | 3306                             | MySQL Docker port             |
| MYSQL_DOCKER_DATABASE                 | java-spring-boot-sql2o-rest-crud | Database name                 |
| MYSQL_DOCKER_USERNAME                 | root                             | Database username             |
| MYSQL_DOCKER_PASSWORD                 |                                  | Database password             |
| VT_NAME_PREFIX                        | vt-jetty-                        | Virtual thread name prefix    |
| REDIS_HOST                            | localhost                        | Redis host                    |
| REDIS_PORT                            | 6379                             | Redis port                    |
| REDIS_PASSWORD                        |                                  | Redis password                |
| RATE_LIMITER_VVIP_MAX_REQUESTS        | 100                              | VVIP tier max requests/sec    |
| RATE_LIMITER_VIP_MAX_REQUESTS         | 50                               | VIP tier max requests/sec     |
| RATE_LIMITER_PREMIUM_MAX_REQUESTS     | 10                               | Premium tier max requests/sec |
| RATE_LIMITER_GENERAL_MAX_REQUESTS     | 1                                | General tier max requests/sec |
| BULKHEAD_MAX_CONCURRENT_CALLS         | 29                               | Max concurrent bulkhead calls |
| BULKHEAD_MAX_WAIT_DURATION            | 100ms                            | Max wait duration             |
| DATA_SEEDER_ENABLED                   | false                            | Enable data seeder scheduler  |
| DATA_SEEDER_CRON                      | 0 \* \* \* \* ?                  | Data seeder cron expression   |
| DATA_SEEDER_TOTAL_COMPANIES           | 10                               | Total companies to seed       |
| DATA_SEEDER_TOTAL_PRODUCTS            | 15                               | Total products to seed        |
| DATA_SEEDER_TOTAL_TUTORIALS           | 10                               | Total tutorials to seed       |
| DATA_SEEDER_MAX_COMPANIES_PER_PRODUCT | 3                                | Max companies per product     |
| SEAT_SEEDER_ENABLED                   | true                             | Enable seat seeder            |

## Key Features

- **UUID v7**: Time-ordered unique identifiers for all entities
- **UUID Validation**: Returns 400 Bad Request for invalid UUID format
- **Virtual Threads**: Java 21 lightweight threads for high scalability
- **API Key Authentication**: All endpoints require x-api-key header (401 if missing/invalid)
- **Redisson Rate Limiting**: Distributed rate limiting with Redis, tiered limits (VVIP/VIP/Premium/General)
- **Resilience4j Bulkhead**: Concurrent database call limiting (29 calls max)
- **HikariCP**: High-performance connection pooling
- **Flyway**: Version-controlled database migrations
- **Sql2o**: Lightweight JDBC wrapper for easy database operations
- **ElSql**: External SQL file management for clean repository code
- **Dynamic Queries**: Dynamic WHERE clause building with ElSql base queries
- **Consolidated Endpoints**: Single endpoint per entity with filter parameters
- **Pagination**: Built-in pagination support with page/size parameters
- **Data Seeder**: Scheduled seeding of real data for companies, products, and tutorials
- **JSON Logging**: Structured JSON logs with traceId support
- **Request Tracing**: X-Trace-Id header for distributed tracing
- **CORS Enabled**: Cross-origin requests allowed for all origins
- **Global Exception Handling**: Consistent error responses
- **Seat Reservation**: Pessimistic locking with FOR UPDATE SKIP LOCKED for deadlock avoidance

## Coding Standards

### Model Classes (Records)

All model classes use Java records for immutability and concise syntax:

```java
public record Product(UUID id, String name, UUID companyID, String companyName, List<Company> companies) {
}
```

### Query Parameter Constants

All SQL parameter names are defined in `ConstantPreference`:

```java
public static final String ID = "id";
public static final String NAME = "name";
public static final String EVENT_ID = "eventId";
public static final String SIZE = "size";
// etc.
```

Usage in repositories:

```java
query.addParameter(ConstantPreference.ID, id.toString());
```

### Try-With-Resources

All database resources (Connection, Query) use try-with-resources for automatic cleanup:

```java
try (Connection conn = sql2o.open();
        Query query = conn.createQuery(sql)) {
    return query.addParameter(ConstantPreference.ID, id.toString())
            .executeAndFetchFirst(Product.class);
}
```

## Response Format

### Paginated Response

All list endpoints return paginated responses:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "first": true,
  "last": false
}
```

### Product Response

```json
{
  "id": "019d2d72-eee3-7b29-9af2-f15d04e4b6d8",
  "name": "Security Services",
  "companyID": "019d2d72-eed8-72b3-bb87-a57c3a1a56b6",
  "companyName": "Singapore Solutions Technologies"
}
```

### Company Response

```json
{
  "id": "019d2d72-eed2-7754-a5b5-1ef67f17fc6c",
  "name": "Singapore Solutions Technologies"
}
```

### Tutorial Response

```json
{
  "id": "019d2d7d-eb52-70ac-87f2-036e33bc4829",
  "title": "Creating Security",
  "description": "Step-by-step tutorial for beginners",
  "published": true
}
```

### Event Response

```json
{
  "id": "019d2d7d-eb52-70ac-87f2-036e33bc4829",
  "name": "Tech Conference 2026",
  "venue": "Convention Center Hall A"
}
```

### Seat Response

```json
{
  "id": "019d2d7d-eb52-70ac-87f2-036e33bc4830",
  "eventId": "019d2d7d-eb52-70ac-87f2-036e33bc4829",
  "seatNumber": "A1",
  "reserved": false,
  "reservationId": null
}
```

### Seat Availability Response

```json
{
  "total": 50,
  "available": 47
}
```

### Reservation Response

```json
{
  "id": "019d2d7d-eb52-70ac-87f2-036e33bc4831",
  "eventId": "019d2d7d-eb52-70ac-87f2-036e33bc4829",
  "customerName": "John Doe",
  "seatCount": 3
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

### Unauthorized Response (401)

```json
{
  "statusCode": 401,
  "timestamp": "2026-03-28T10:30:00",
  "message": "Missing required header: x-api-key",
  "description": "uri=/api/products"
}
```

### Rate Limit Exceeded Response (429)

```json
{
  "statusCode": 429,
  "timestamp": "2026-03-28T10:30:00",
  "message": "Rate limit exceeded for tier: vvip",
  "description": "uri=/api/products"
}
```

## Log Output Format

### Request Start

```json
{
  "timestamp": "2026-03-25T19:17:48.485351932+07:00",
  "level": "INFO",
  "thread_name": "vt-jetty-1",
  "message": "Request started",
  "caller": {
    "class": "com.otis.config.TraceIdFilter",
    "method": "doFilterInternal",
    "file": "TraceIdFilter.java",
    "line": 49
  },
  "traceId": "19d24edf7c5841abf",
  "method": "GET",
  "event": "START",
  "uri": "/api/products"
}
```

### Request End

```json
{
  "timestamp": "2026-03-25T19:17:48.841024008+07:00",
  "level": "INFO",
  "thread_name": "vt-jetty-1",
  "message": "Request completed",
  "caller": {
    "class": "com.otis.config.TraceIdFilter",
    "method": "doFilterInternal",
    "file": "TraceIdFilter.java",
    "line": 61
  },
  "traceId": "19d24edf7c5841abf",
  "method": "GET",
  "event": "END",
  "uri": "/api/products",
  "processTime": "355 ms",
  "status": "200"
}
```

### Database Query

```json
{
  "timestamp": "2026-03-25T19:17:48.523154262+07:00",
  "level": "INFO",
  "thread_name": "vt-jetty-1",
  "message": "GetAllProduct: SELECT id, name, company_id FROM products ",
  "caller": {
    "class": "com.otis.repository.ProductRepository",
    "method": "findAll",
    "file": "ProductRepository.java",
    "line": 34
  },
  "traceId": "19d24edf7c5841abf",
  "method": "GET",
  "event": "START",
  "uri": "/api/products"
}
```

## Stress Test Report

A stress test was conducted to verify the rate limiting functionality. The test sends 1000 concurrent requests per API key across all endpoints with different rate limiting tiers.

### Test Configuration

| Parameter        | Value                                                      |
| ---------------- | ---------------------------------------------------------- |
| Total Requests   | 1000 per key                                               |
| Concurrency      | 10                                                         |
| Endpoints Tested | /api/products, /api/companies, /api/tutorials, /api/events |

### Test Results Summary

The rate limiter successfully enforces per-tier limits across all API keys:

| Tier    | Max Requests/sec | Actual req/sec | Rate-Limited |
| ------- | ---------------- | -------------- | ------------ |
| VVIP    | 100              | 1,500 - 7,200  | ~90-100%     |
| VIP     | 50               | 2,800 - 7,700  | ~95-100%     |
| Premium | 10               | 3,500 - 7,500  | ~99-100%     |
| General | 1                | 3,700 - 7,000  | ~99-100%     |

### Detailed Report

![Stress Test Report](etc/image/report.png)

Full report available at: `reports/20260329_060541/report.html`

## License

Propriety of Otis
