package com.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "search_history_records", indexes = {
    @Index(name = "idx_search_history_user_used", columnList = "user_id, last_used_at"),
    @Index(name = "idx_search_history_user_saved", columnList = "user_id, saved")
})
public class SearchHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String keyword;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @Column(name = "target_path", length = 500)
    private String targetPath;

    @Column(name = "query_payload", columnDefinition = "TEXT")
    private String queryPayload;

    @Column(nullable = false)
    private Boolean saved = false;

    @Column(length = 120)
    private String label;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 1;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime timestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
