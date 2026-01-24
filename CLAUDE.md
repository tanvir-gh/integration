# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot microservices demonstrating integration patterns in the entertainment domain:

- **catalog-service** (port 8080): Source of truth for content metadata (movies, series)
- **watch-history-service** (port 8081): Tracks user viewing activity

## Build Commands

```bash
# Build a service
cd catalog-service && ./gradlew build
cd watch-history-service && ./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "CatalogServiceIntegrationTest"

# Run a single test method
./gradlew test --tests "CatalogServiceIntegrationTest.shouldCreateContent"
```

## Running Locally

```bash
# Start infrastructure
docker compose up -d postgres kafka

# Start all services
docker compose up -d

# View logs
docker compose logs -f catalog-service watch-history-service

# Reset everything
docker compose down -v
```

## Architecture

### Technology Stack
- Java 21, Spring Boot 3.5.8, Gradle 9.3.0
- PostgreSQL (separate schemas: `catalog_db`, `watch_history_db`)
- Apache Kafka for async messaging
- Testcontainers + Awaitility for integration tests

### Integration Patterns

**Kafka (async):** watch-history-service publishes `WatchEvent` to `watch-events` topic when recording a watch

**HTTP (sync):** watch-history-service calls catalog-service via RestClient to enrich history with content details

```
POST /api/watch → saves WatchRecord → publishes WatchEvent to Kafka
GET /api/history/{visitorId} → fetches WatchRecords → calls GET /api/catalog/batch?ids=... → returns enriched history
```

### Code Patterns

**Entities** use Lombok annotations and JPA with schema-qualified tables:
```java
@Entity
@Table(name = "content", schema = "catalog_db")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Content { ... }
```

**Kafka producers** use KafkaTemplate with JSON serialization via ObjectMapper

**HTTP clients** use Spring's RestClient with @Value-injected base URL:
```java
@Component
public class CatalogServiceClient {
    public CatalogServiceClient(@Value("${catalog-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }
}
```

### API Endpoints

**Catalog Service (8080):**
- `POST /api/catalog` - Create content
- `GET /api/catalog/{id}` - Get by ID
- `GET /api/catalog` - List all
- `GET /api/catalog/batch?ids=1,2,3` - Batch fetch

**Watch History Service (8081):**
- `POST /api/watch` - Record watch event (publishes to Kafka)
- `GET /api/history/{visitorId}` - Get enriched history (calls catalog via HTTP)

## Testing

Integration tests use Testcontainers (PostgreSQL, Kafka) and run automatically with `./gradlew test`. Tests are in:
- `catalog-service/src/test/java/com/entertainment/catalog/`
- `watch-history-service/src/test/java/com/entertainment/watchhistory/`

## Configuration

- `application.yml` in each service for runtime config
- `application-test.yml` for test overrides
- `docker-compose.yml` sets environment variables for containerized deployment
- Copy `.env.example` to `.env` for New Relic configuration
