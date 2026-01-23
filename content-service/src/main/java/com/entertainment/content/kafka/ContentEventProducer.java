package com.entertainment.content.kafka;

import com.entertainment.content.domain.Content;
import com.entertainment.content.event.ContentCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentEventProducer {

    private static final String TOPIC = "content-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishContentCreated(Content content) {
        ContentCreatedEvent event = ContentCreatedEvent.builder()
                .id(content.getId())
                .title(content.getTitle())
                .type(content.getType().name())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, content.getId().toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published content created event for id: {}", content.getId());
                        } else {
                            log.error("Failed to publish content created event for id: {}", content.getId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize content created event", e);
        }
    }
}
