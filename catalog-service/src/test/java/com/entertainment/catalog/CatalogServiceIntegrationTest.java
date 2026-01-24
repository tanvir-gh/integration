package com.entertainment.catalog;

import com.entertainment.catalog.domain.Content;
import com.entertainment.catalog.domain.ContentType;
import com.entertainment.catalog.repository.ContentRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CatalogServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("entertainment")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
    }

    @Test
    void shouldCreateContent() throws Exception {
        String requestBody = """
                {
                    "title": "Inception",
                    "type": "MOVIE",
                    "durationMinutes": 148,
                    "genre": "ACTION"
                }
                """;

        mockMvc.perform(post("/api/catalog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.type").value("MOVIE"))
                .andExpect(jsonPath("$.durationMinutes").value(148))
                .andExpect(jsonPath("$.genre").value("ACTION"))
                .andExpect(jsonPath("$.publishedAt").exists());

        List<Content> contents = contentRepository.findAll();
        assertThat(contents).hasSize(1);
        assertThat(contents.getFirst().getTitle()).isEqualTo("Inception");
        assertThat(contents.getFirst().getDurationMinutes()).isEqualTo(148);
    }

    @Test
    void shouldGetContentById() throws Exception {
        Content content = Content.builder()
                .title("The Matrix")
                .type(ContentType.MOVIE)
                .durationMinutes(136)
                .genre("ACTION")
                .build();
        Content saved = contentRepository.save(content);

        mockMvc.perform(get("/api/catalog/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("The Matrix"))
                .andExpect(jsonPath("$.durationMinutes").value(136));
    }

    @Test
    void shouldReturnNotFoundForNonExistentContent() throws Exception {
        mockMvc.perform(get("/api/catalog/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListAllContent() throws Exception {
        contentRepository.save(Content.builder()
                .title("Movie 1")
                .type(ContentType.MOVIE)
                .durationMinutes(120)
                .genre("COMEDY")
                .build());
        contentRepository.save(Content.builder()
                .title("Series 1")
                .type(ContentType.SERIES)
                .durationMinutes(45)
                .genre("DRAMA")
                .build());

        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetContentBatch() throws Exception {
        Content content1 = contentRepository.save(Content.builder()
                .title("Movie 1")
                .type(ContentType.MOVIE)
                .durationMinutes(120)
                .genre("ACTION")
                .build());
        Content content2 = contentRepository.save(Content.builder()
                .title("Movie 2")
                .type(ContentType.MOVIE)
                .durationMinutes(90)
                .genre("COMEDY")
                .build());
        contentRepository.save(Content.builder()
                .title("Movie 3")
                .type(ContentType.MOVIE)
                .durationMinutes(100)
                .genre("DRAMA")
                .build());

        mockMvc.perform(get("/api/catalog/batch")
                        .param("ids", content1.getId().toString(), content2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == 'Movie 1')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Movie 2')]").exists());
    }
}
