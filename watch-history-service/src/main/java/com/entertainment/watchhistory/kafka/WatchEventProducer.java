package com.entertainment.watchhistory.kafka;

import com.entertainment.watchhistory.domain.WatchRecord;
import com.entertainment.watchhistory.event.WatchEvent;
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
public class WatchEventProducer {

    private static final String TOPIC = "watch-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishWatchEvent(WatchRecord watchRecord) {
        WatchEvent event = WatchEvent.builder()
                .visitorId(watchRecord.getVisitorId())
                .contentId(watchRecord.getContentId())
                .watchedSeconds(watchRecord.getWatchedSeconds())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, watchRecord.getVisitorId(), message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published watch event for visitor: {}, content: {}",
                                    watchRecord.getVisitorId(), watchRecord.getContentId());
                        } else {
                            log.error("Failed to publish watch event for visitor: {}",
                                    watchRecord.getVisitorId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize watch event", e);
        }
    }
}
