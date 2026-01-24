# Entertainment Microservices Integration Project

## Project Overview

This is a Spring Boot microservices project demonstrating integration patterns between two services in the entertainment domain:

- **catalog-service** (port 8080): Source of truth for content metadata (movies, series)
- **watch-history-service** (port 8081): Tracks user viewing activity

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.8
- **Build Tool**: Gradle 9.3.0 (Groovy DSL)
- **Database**: PostgreSQL (shared instance, separate schemas: `catalog_db`, `watch_history_db`)
- **Messaging**: Apache Kafka
- **Containerization**: Docker Compose
- **Testing**: JUnit 5, Testcontainers, Awaitility
- **Observability**: New Relic APM, Prometheus metrics, structured logging

## Project Structure

```
integration/
├── docker-compose.yml          # Orchestrates all services
├── init-db.sql                 # Database schema initialization
├── .env.example                # Environment variables template
├── catalog-service/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── newrelic/newrelic.yml
│   └── src/main/java/com/entertainment/catalog/
│       ├── controller/         # REST endpoints
│       ├── service/            # Business logic
│       ├── repository/         # JPA repositories
│       └── domain/             # Entity classes
└── watch-history-service/
    ├── build.gradle
    ├── Dockerfile
    ├── newrelic/newrelic.yml
    └── src/main/java/com/entertainment/watchhistory/
        ├── controller/         # REST endpoints
        ├── service/            # Business logic
        ├── repository/         # JPA repositories
        ├── domain/             # Entity classes
        ├── client/             # HTTP client for catalog-service
        ├── event/              # Kafka event DTOs
        └── kafka/              # Kafka producer
```

## Integration Patterns

### Asynchronous (Kafka)
- **Topic**: `watch-events`
- **Flow**: watch-history-service publishes `WatchEvent` when user watches content
- **Purpose**: Fire-and-forget event publishing for analytics/downstream consumers

### Synchronous (HTTP)
- watch-history-service calls catalog-service via `RestClient` to fetch content details
- Base URL configured via `catalog-service.url` property
- Batch endpoint for efficient fetching: `GET /api/catalog/batch?ids=1,2,3`

## Build Commands

```bash
# Build both services
cd catalog-service && ./gradlew build
cd watch-history-service && ./gradlew build

# Run tests
cd catalog-service && ./gradlew test
cd watch-history-service && ./gradlew test
```

## Running the Application

```bash
# Start infrastructure (PostgreSQL, Kafka)
docker-compose up -d postgres kafka

# Start all services including New Relic infrastructure
docker-compose up -d

# View logs
docker-compose logs -f catalog-service watch-history-service
```

## Testing

### Integration Tests
Both services use Testcontainers for integration testing:
- PostgreSQL container for database tests
- Kafka container for messaging tests (watch-history-service)
- Awaitility for async operation verification

### API Testing
```bash
# Add content to catalog
curl -X POST http://localhost:8080/api/catalog \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","type":"MOVIE","durationMinutes":148,"genre":"ACTION"}'

# Record a watch event (publishes to Kafka)
curl -X POST http://localhost:8081/api/watch \
  -H "Content-Type: application/json" \
  -d '{"visitorId":"visitor-123","contentId":1,"watchedSeconds":3600}'

# Get watch history (calls catalog via HTTP)
curl http://localhost:8081/api/history/visitor-123
```

## API Endpoints

### Catalog Service (port 8080)
- `POST /api/catalog` - Add content to catalog
- `GET /api/catalog/{id}` - Get content by ID
- `GET /api/catalog` - List all content
- `GET /api/catalog/batch?ids=1,2,3` - Batch fetch content by IDs
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

### Watch History Service (port 8081)
- `POST /api/watch` - Record a watch event (publishes to Kafka)
- `GET /api/history/{visitorId}` - Get watch history for visitor (calls catalog via HTTP)
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

## Domain Models

### Content (catalog-service)
```java
@Entity
@Table(name = "content", schema = "catalog_db")
public class Content {
    Long id;
    String title;
    ContentType type;        // MOVIE, SERIES
    Integer durationMinutes; // Runtime for movies, avg episode length for series
    String genre;            // ACTION, COMEDY, DRAMA, etc.
    LocalDateTime publishedAt;
}
```

### WatchRecord (watch-history-service)
```java
@Entity
@Table(name = "watch_record", schema = "watch_history_db")
public class WatchRecord {
    Long id;
    String visitorId;        // Anonymous visitor identifier
    Long contentId;          // Reference to catalog content
    Integer watchedSeconds;  // How long they watched
    LocalDateTime watchedAt;
}
```

## Integration Flow

```
1. User watches content:
   POST /api/watch {visitorId: "v123", contentId: 1, watchedSeconds: 3600}

2. watch-history-service:
   - Saves WatchRecord to database
   - Publishes WatchEvent to Kafka (fire-and-forget)
   - Returns success

3. User views their history:
   GET /api/history/v123

4. watch-history-service:
   - Fetches WatchRecords from database
   - Calls catalog-service HTTP: GET /api/catalog/batch?ids=1,2,3
   - Merges content details with watch records
   - Returns enriched history
```

## Observability

### New Relic APM
- Java agent included in Docker images
- Distributed tracing enabled across services
- Log forwarding with trace context
- Configure via `NEW_RELIC_LICENSE_KEY` environment variable

### New Relic Infrastructure
- Docker container monitoring via `newrelic-infra` container
- Requires `NEW_RELIC_LICENSE_KEY` in `.env`

### Prometheus Metrics
- Exposed at `/actuator/prometheus`
- Micrometer integration for Spring Boot metrics

### Structured Logging
- Logstash Logback Encoder for JSON logging
- Activate with `spring.profiles.active=json`

## Environment Variables

Copy `.env.example` to `.env` and configure:
```
NEW_RELIC_LICENSE_KEY=your_license_key_here
NEW_RELIC_ACCOUNT_ID=your_account_id_here
```

## Key Code Patterns

### Entity Definition
```java
@Entity
@Table(name = "content", schema = "catalog_db")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Enumerated(EnumType.STRING)
    private ContentType type;
    private Integer durationMinutes;
    private String genre;
    private LocalDateTime publishedAt;
}
```

### Kafka Producer
```java
@Component
@RequiredArgsConstructor
public class WatchEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishWatchEvent(WatchRecord watchRecord) {
        WatchEvent event = WatchEvent.builder()
                .visitorId(watchRecord.getVisitorId())
                .contentId(watchRecord.getContentId())
                .watchedSeconds(watchRecord.getWatchedSeconds())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send("watch-events", watchRecord.getVisitorId(),
                objectMapper.writeValueAsString(event));
    }
}
```

### HTTP Client (RestClient)
```java
@Component
public class CatalogServiceClient {
    private final RestClient restClient;

    public CatalogServiceClient(@Value("${catalog-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<ContentResponse> getContentBatch(List<Long> ids) {
        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return restClient.get()
                .uri("/api/catalog/batch?ids={ids}", idsParam)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
```

## Database Schemas

- `catalog_db`: Used by catalog-service
- `watch_history_db`: Used by watch-history-service

Both schemas are created automatically via `init-db.sql` when PostgreSQL starts.

## Troubleshooting

### Services not starting
1. Ensure Docker is running
2. Check if ports 8080, 8081, 5432, 9092 are available
3. Run `docker-compose down -v` to reset volumes

### Kafka connection issues
- Wait for Kafka to be fully ready before starting services
- Check `docker-compose logs kafka`

### New Relic not reporting
- Verify `NEW_RELIC_LICENSE_KEY` is set in `.env`
- Check service logs for New Relic agent messages
