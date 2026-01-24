package com.entertainment.watchhistory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_record", schema = "watch_history_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false)
    private String visitorId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    @PrePersist
    protected void onCreate() {
        watchedAt = LocalDateTime.now();
    }
}
