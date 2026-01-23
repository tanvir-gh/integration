package com.entertainment.content.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreatedEvent {
    private Long id;
    private String title;
    private String type;
    private LocalDateTime timestamp;
}
