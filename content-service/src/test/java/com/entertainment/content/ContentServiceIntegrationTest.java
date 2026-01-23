package com.entertainment.content;

import com.entertainment.content.domain.Content;
import com.entertainment.content.domain.ContentType;
import com.entertainment.content.event.ContentCreatedEvent;
import com.entertainment.content.repository.ContentRepository;
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
class ContentServiceIntegrationTest {

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
    private ContentRepository contentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
    }

    @Test
    void shouldCreateContent() throws Exception {
        String requestBody = """
                {
                    "title": "The Matrix",
                    "type": "MOVIE"
                }
                """;

        mockMvc.perform(post("/api/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("The Matrix"))
                .andExpect(jsonPath("$.type").value("MOVIE"))
                .andExpect(jsonPath("$.createdAt").exists());

        List<Content> contents = contentRepository.findAll();
        assertThat(contents).hasSize(1);
        assertThat(contents.getFirst().getTitle()).isEqualTo("The Matrix");
    }

    @Test
    void shouldGetContentById() throws Exception {
        Content content = Content.builder()
                .title("Inception")
                .type(ContentType.MOVIE)
                .build();
        Content saved = contentRepository.save(content);

        mockMvc.perform(get("/api/content/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentContent() throws Exception {
        mockMvc.perform(get("/api/content/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListAllContent() throws Exception {
        contentRepository.save(Content.builder().title("Movie 1").type(ContentType.MOVIE).build());
        contentRepository.save(Content.builder().title("Show 1").type(ContentType.SHOW).build());

        mockMvc.perform(get("/api/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldPublishKafkaEventOnContentCreation() throws Exception {
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of("content-events"));
            // Poll once to ensure consumer is assigned to partitions
            consumer.poll(Duration.ofMillis(1000));

            String requestBody = """
                    {
                        "title": "Breaking Bad",
                        "type": "SHOW"
                    }
                    """;

            mockMvc.perform(post("/api/content")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            await().atMost(30, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.count()).isGreaterThan(0);

                String message = records.iterator().next().value();
                ContentCreatedEvent event = objectMapper.readValue(message, ContentCreatedEvent.class);
                assertThat(event.getTitle()).isEqualTo("Breaking Bad");
                assertThat(event.getType()).isEqualTo("SHOW");
            });
        }
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
