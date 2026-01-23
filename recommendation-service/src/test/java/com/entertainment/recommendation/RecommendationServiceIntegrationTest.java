package com.entertainment.recommendation;

import com.entertainment.recommendation.domain.ContentView;
import com.entertainment.recommendation.repository.ContentViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RecommendationServiceIntegrationTest {

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
    private ContentViewRepository contentViewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        contentViewRepository.deleteAll();
    }

    @Test
    void shouldConsumeContentCreatedEventAndStoreContentView() throws Exception {
        String event = """
                {
                    "id": 1,
                    "title": "The Matrix",
                    "type": "MOVIE",
                    "timestamp": "%s"
                }
                """.formatted(LocalDateTime.now().toString());

        try (KafkaProducer<String, String> producer = createKafkaProducer()) {
            producer.send(new ProducerRecord<>("content-events", "1", event)).get();
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ContentView> views = contentViewRepository.findAll();
            assertThat(views).hasSize(1);
            assertThat(views.getFirst().getContentTitle()).isEqualTo("The Matrix");
            assertThat(views.getFirst().getContentType()).isEqualTo("MOVIE");
            assertThat(views.getFirst().getContentId()).isEqualTo(1L);
        });
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenNoContentViews() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnContentViews() throws Exception {
        ContentView view1 = ContentView.builder()
                .contentId(1L)
                .contentTitle("The Matrix")
                .contentType("MOVIE")
                .build();
        ContentView view2 = ContentView.builder()
                .contentId(2L)
                .contentTitle("Breaking Bad")
                .contentType("SHOW")
                .build();

        contentViewRepository.saveAll(List.of(view1, view2));

        mockMvc.perform(get("/api/content-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnRecommendationsBasedOnContentViews() throws Exception {
        ContentView view = ContentView.builder()
                .contentId(1L)
                .contentTitle("Inception")
                .contentType("MOVIE")
                .build();
        contentViewRepository.save(view);

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contentId").value(1))
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[0].type").value("MOVIE"));
    }

    @Test
    void shouldConsumeMultipleEventsAndMaintainOrder() throws Exception {
        try (KafkaProducer<String, String> producer = createKafkaProducer()) {
            for (int i = 1; i <= 5; i++) {
                String event = """
                        {
                            "id": %d,
                            "title": "Content %d",
                            "type": "MOVIE",
                            "timestamp": "%s"
                        }
                        """.formatted(i, i, LocalDateTime.now().toString());
                producer.send(new ProducerRecord<>("content-events", String.valueOf(i), event)).get();
            }
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ContentView> views = contentViewRepository.findAll();
            assertThat(views).hasSize(5);
        });

        mockMvc.perform(get("/api/content-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        ));
    }
}
