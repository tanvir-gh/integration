package com.entertainment.watchhistory;

import com.entertainment.watchhistory.domain.WatchRecord;
import com.entertainment.watchhistory.event.WatchEvent;
import com.entertainment.watchhistory.repository.WatchRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class WatchHistoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("entertainment")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-schema.sql");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WatchRecordRepository watchRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        watchRecordRepository.deleteAll();
    }

    @Test
    void shouldRecordWatchEvent() throws Exception {
        String requestBody = """
                {
                    "visitorId": "visitor-123",
                    "contentId": 1,
                    "watchedSeconds": 3600
                }
                """;

        mockMvc.perform(post("/api/watch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.visitorId").value("visitor-123"))
                .andExpect(jsonPath("$.contentId").value(1))
                .andExpect(jsonPath("$.watchedSeconds").value(3600))
                .andExpect(jsonPath("$.watchedAt").exists());

        List<WatchRecord> records = watchRecordRepository.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getVisitorId()).isEqualTo("visitor-123");
        assertThat(records.getFirst().getWatchedSeconds()).isEqualTo(3600);
    }

    @Test
    void shouldPublishKafkaEventOnWatch() throws Exception {
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of("watch-events"));
            consumer.poll(Duration.ofMillis(1000));

            String requestBody = """
                    {
                        "visitorId": "visitor-456",
                        "contentId": 2,
                        "watchedSeconds": 1800
                    }
                    """;

            mockMvc.perform(post("/api/watch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            await().atMost(30, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.count()).isGreaterThan(0);

                String message = records.iterator().next().value();
                WatchEvent event = objectMapper.readValue(message, WatchEvent.class);
                assertThat(event.getVisitorId()).isEqualTo("visitor-456");
                assertThat(event.getContentId()).isEqualTo(2L);
                assertThat(event.getWatchedSeconds()).isEqualTo(1800);
            });
        }
    }

    @Test
    void shouldReturnEmptyHistoryForNewVisitor() throws Exception {
        mockMvc.perform(get("/api/history/unknown-visitor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnWatchHistoryForVisitor() throws Exception {
        WatchRecord record1 = WatchRecord.builder()
                .visitorId("visitor-789")
                .contentId(1L)
                .watchedSeconds(3600)
                .build();
        WatchRecord record2 = WatchRecord.builder()
                .visitorId("visitor-789")
                .contentId(2L)
                .watchedSeconds(1800)
                .build();
        watchRecordRepository.saveAll(List.of(record1, record2));

        mockMvc.perform(get("/api/history/visitor-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRecordMultipleWatchEventsForSameVisitor() throws Exception {
        String requestBody1 = """
                {
                    "visitorId": "visitor-multi",
                    "contentId": 1,
                    "watchedSeconds": 1000
                }
                """;
        String requestBody2 = """
                {
                    "visitorId": "visitor-multi",
                    "contentId": 2,
                    "watchedSeconds": 2000
                }
                """;

        mockMvc.perform(post("/api/watch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/watch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody2))
                .andExpect(status().isCreated());

        List<WatchRecord> records = watchRecordRepository.findByVisitorIdOrderByWatchedAtDesc("visitor-multi");
        assertThat(records).hasSize(2);
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }
}
