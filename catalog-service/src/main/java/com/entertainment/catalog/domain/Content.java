package com.entertainment.catalog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "content", schema = "catalog_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentType type;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column
    private String genre;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        publishedAt = LocalDateTime.now();
    }
}
