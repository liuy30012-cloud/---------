package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_suggestions", indexes = {
    @Index(name = "idx_query", columnList = "query"),
    @Index(name = "idx_frequency", columnList = "frequency DESC")
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class SearchSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String query;

    @Column(name = "normalized_query")
    private String normalizedQuery;

    @Column(nullable = false)
    private Integer frequency = 1;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
