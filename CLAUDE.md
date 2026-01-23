# Entertainment Microservices Integration Project

## Project Overview

This is a Spring Boot microservices project demonstrating integration patterns between two services in the entertainment domain:

- **content-service** (port 8080): Manages entertainment content (movies, shows)
- **recommendation-service** (port 8081): Provides recommendations based on content events

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.8
- **Build Tool**: Gradle 9.3.0 (Groovy DSL)
- **Database**: PostgreSQL (shared instance, separate schemas: `content_db`, `recommendation_db`)
- **Messaging**: Apache Kafka
- **Containerization**: Docker Compose
- **Testing**: JUnit 5, Testcontainers, Awaitility
- **Observability**: New Relic APM, Prometheus metrics, structured logging

## Project Structure

```
integration/
├── docker-compose.yml          # Orchestrates all services
├── init-db.sql                 # Database schema initialization
├── test-api.sh                 # API testing and trace generation script
├── .env.example                # Environment variables template
├── content-service/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── newrelic/newrelic.yml
│   └── src/main/java/com/entertainment/content/
│       ├── controller/         # REST endpoints
│       ├── service/            # Business logic
│       ├── repository/         # JPA repositories
│       ├── domain/             # Entity classes
│       ├── event/              # Kafka event DTOs
│       └── kafka/              # Kafka producer
└── recommendation-service/
    ├── build.gradle
    ├── Dockerfile
    ├── newrelic/newrelic.yml
    └── src/main/java/com/entertainment/recommendation/
        ├── controller/         # REST endpoints
        ├── service/            # Business logic
        ├── repository/         # JPA repositories
        ├── domain/             # Entity classes
        ├── client/             # HTTP client for content-service
        └── kafka/              # Kafka consumer
```

## Integration Patterns

### Asynchronous (Kafka)
- **Topic**: `content-events`
- **Flow**: content-service publishes `ContentCreatedEvent` when content is created
- **Consumer**: recommendation-service listens and stores content references

### Synchronous (HTTP)
- recommendation-service calls content-service via `RestClient` to fetch content details
- Base URL configured via `content-service.url` property

## Build Commands

```bash
# Build both services
./gradlew build

# Build specific service
cd content-service && ./gradlew build
cd recommendation-service && ./gradlew build

# Run tests
./gradlew test
```

## Running the Application

```bash
# Start infrastructure (PostgreSQL, Kafka)
docker-compose up -d postgres kafka

# Start all services including New Relic infrastructure
docker-compose up -d

# View logs
docker-compose logs -f content-service recommendation-service
```

## Testing

### Integration Tests
Both services use Testcontainers for integration testing:
- PostgreSQL container for database tests
- Kafka container for messaging tests
- Awaitility for async operation verification

### API Testing
```bash
# Run the test script (generates traces)
./test-api.sh 10

# Manual testing
curl -X POST http://localhost:8080/api/content \
  -H "Content-Type: application/json" \
  -d '{"title":"The Matrix","type":"MOVIE"}'

curl http://localhost:8081/api/recommendations
```

## API Endpoints

### Content Service (port 8080)
- `POST /api/content` - Create content (publishes Kafka event)
- `GET /api/content/{id}` - Get content by ID
- `GET /api/content` - List all content
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

### Recommendation Service (port 8081)
- `GET /api/recommendations` - Get recommendations (calls content-service)
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

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
@Table(name = "content", schema = "content_db")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Enumerated(EnumType.STRING)
    private ContentType type;
    private LocalDateTime createdAt;
}
```

### Kafka Producer
```java
@Service
@RequiredArgsConstructor
public class ContentEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendContentCreatedEvent(ContentCreatedEvent event) {
        kafkaTemplate.send("content-events", objectMapper.writeValueAsString(event));
    }
}
```

### Kafka Consumer
```java
@Service
@RequiredArgsConstructor
public class ContentEventConsumer {
    @KafkaListener(topics = "content-events", groupId = "recommendation-service")
    public void handleContentCreated(String message) {
        // Process event
    }
}
```

### HTTP Client (RestClient)
```java
@Service
public class ContentServiceClient {
    private final RestClient restClient;

    public ContentServiceClient(@Value("${content-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ContentDto getContent(Long id) {
        return restClient.get()
            .uri("/api/content/{id}", id)
            .retrieve()
            .body(ContentDto.class);
    }
}
```

## Database Schemas

- `content_db`: Used by content-service
- `recommendation_db`: Used by recommendation-service

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
