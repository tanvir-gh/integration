package com.entertainment.catalog;

import com.entertainment.catalog.domain.Content;
import com.entertainment.catalog.domain.ContentType;
import com.entertainment.catalog.repository.ContentRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("l2cache")
class L2CacheIntegrationTest {

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
    private ContentRepository contentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Statistics getStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
        getStatistics().clear();
    }

    @Test
    void shouldCacheEntityOnSecondLevelCache() {
        Statistics stats = getStatistics();

        Long contentId = transactionTemplate.execute(status -> {
            Content content = Content.builder()
                    .title("Cached Movie")
                    .type(ContentType.MOVIE)
                    .durationMinutes(120)
                    .genre("ACTION")
                    .build();
            return contentRepository.save(content).getId();
        });

        transactionTemplate.executeWithoutResult(status -> {
            contentRepository.findById(contentId).orElseThrow();
        });

        long missAfterFirstFetch = stats.getSecondLevelCacheMissCount();
        long putAfterFirstFetch = stats.getSecondLevelCachePutCount();

        transactionTemplate.executeWithoutResult(status -> {
            Content fetched = contentRepository.findById(contentId).orElseThrow();
            assertThat(fetched.getTitle()).isEqualTo("Cached Movie");
        });

        long hitsAfterSecondFetch = stats.getSecondLevelCacheHitCount();
        long missAfterSecondFetch = stats.getSecondLevelCacheMissCount();

        assertThat(putAfterFirstFetch).as("Entity should be put into cache after first fetch")
                .isGreaterThan(0);
        assertThat(hitsAfterSecondFetch).as("Second fetch should hit the cache")
                .isGreaterThan(0);
        assertThat(missAfterSecondFetch).as("Miss count should not increase after cache is populated")
                .isEqualTo(missAfterFirstFetch);
    }

    @Test
    void shouldReportCacheStatistics() {
        Statistics stats = getStatistics();
        assertThat(stats.isStatisticsEnabled()).isTrue();

        assertThat(stats.getSecondLevelCacheHitCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getSecondLevelCacheMissCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getSecondLevelCachePutCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldHaveSecondLevelCacheEnabled() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        assertThat(sessionFactory.getCache()).isNotNull();
        assertThat(sessionFactory.getSessionFactoryOptions().isSecondLevelCacheEnabled()).isTrue();
    }
}
