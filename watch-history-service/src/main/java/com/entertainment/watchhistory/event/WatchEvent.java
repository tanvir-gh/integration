package com.entertainment.watchhistory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchEvent {
    private String visitorId;
    private Long contentId;
    private Integer watchedSeconds;
    private LocalDateTime timestamp;
}
