package com.entertainment.recommendation.kafka;

import com.entertainment.recommendation.domain.ContentView;
import com.entertainment.recommendation.repository.ContentViewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentEventConsumer {

    private final ContentViewRepository contentViewRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "content-events", groupId = "recommendation-service")
    public void consumeContentCreated(String message) {
        log.info("Received content event: {}", message);
        try {
            ContentCreatedEvent event = objectMapper.readValue(message, ContentCreatedEvent.class);

            ContentView contentView = ContentView.builder()
                    .contentId(event.id())
                    .contentTitle(event.title())
                    .contentType(event.type())
                    .build();

            contentViewRepository.save(contentView);
            log.info("Saved content view for content id: {}", event.id());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse content event", e);
        }
    }

    public record ContentCreatedEvent(Long id, String title, String type, String timestamp) {}
}
