package com.entertainment.watchhistory.controller;

import com.entertainment.watchhistory.domain.WatchRecord;
import com.entertainment.watchhistory.service.WatchHistoryService;
import com.entertainment.watchhistory.service.WatchHistoryService.WatchHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @PostMapping("/watch")
    public ResponseEntity<WatchRecord> recordWatch(@RequestBody RecordWatchRequest request) {
        WatchRecord watchRecord = WatchRecord.builder()
                .visitorId(request.visitorId())
                .contentId(request.contentId())
                .watchedSeconds(request.watchedSeconds())
                .build();
        WatchRecord saved = watchHistoryService.recordWatch(watchRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/history/{visitorId}")
    public ResponseEntity<List<WatchHistoryResponse>> getWatchHistory(@PathVariable String visitorId) {
        return ResponseEntity.ok(watchHistoryService.getWatchHistory(visitorId));
    }

    public record RecordWatchRequest(String visitorId, Long contentId, Integer watchedSeconds) {}
}
