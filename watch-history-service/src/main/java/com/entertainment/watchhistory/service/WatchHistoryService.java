package com.entertainment.watchhistory.service;

import com.entertainment.watchhistory.client.CatalogServiceClient;
import com.entertainment.watchhistory.client.CatalogServiceClient.ContentResponse;
import com.entertainment.watchhistory.domain.WatchRecord;
import com.entertainment.watchhistory.kafka.WatchEventProducer;
import com.entertainment.watchhistory.repository.WatchRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryService {

    private final WatchRecordRepository watchRecordRepository;
    private final WatchEventProducer watchEventProducer;
    private final CatalogServiceClient catalogServiceClient;

    @Transactional
    public WatchRecord recordWatch(WatchRecord watchRecord) {
        WatchRecord saved = watchRecordRepository.save(watchRecord);
        log.info("Recorded watch for visitor: {}, content: {}",
                saved.getVisitorId(), saved.getContentId());
        watchEventProducer.publishWatchEvent(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WatchHistoryResponse> getWatchHistory(String visitorId) {
        List<WatchRecord> records = watchRecordRepository.findByVisitorIdOrderByWatchedAtDesc(visitorId);

        if (records.isEmpty()) {
            return List.of();
        }

        List<Long> contentIds = records.stream()
                .map(WatchRecord::getContentId)
                .distinct()
                .toList();

        Map<Long, ContentResponse> contentMap = catalogServiceClient.getContentBatch(contentIds)
                .stream()
                .collect(Collectors.toMap(ContentResponse::id, Function.identity()));

        return records.stream()
                .map(record -> {
                    ContentResponse content = contentMap.get(record.getContentId());
                    return new WatchHistoryResponse(
                            record.getId(),
                            record.getVisitorId(),
                            record.getContentId(),
                            content != null ? content.title() : null,
                            content != null ? content.type() : null,
                            content != null ? content.genre() : null,
                            content != null ? content.durationMinutes() : null,
                            record.getWatchedSeconds(),
                            record.getWatchedAt().toString()
                    );
                })
                .toList();
    }

    public record WatchHistoryResponse(
            Long id,
            String visitorId,
            Long contentId,
            String contentTitle,
            String contentType,
            String genre,
            Integer durationMinutes,
            Integer watchedSeconds,
            String watchedAt
    ) {}
}
