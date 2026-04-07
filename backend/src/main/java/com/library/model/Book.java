package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "books", indexes = {
    @Index(name = "idx_title", columnList = "title"),
    @Index(name = "idx_author", columnList = "author"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_isbn", columnList = "isbn"),
    @Index(name = "idx_search_composite", columnList = "title,author,category,available_copies"),
    @Index(name = "idx_category_status", columnList = "category,available_copies"),
    @Index(name = "idx_language_category", columnList = "language_code,category")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(length = 50)
    private String isbn;

    @Column(length = 100)
    private String location;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(length = 50)
    private String status;

    @Column(name = "publish_year", length = 20)
    private String year;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(length = 50)
    private String availability;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "circulation_policy", nullable = false, length = 20)
    private CirculationPolicy circulationPolicy = CirculationPolicy.AUTO;

    // 库存管理字段
    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies = 1;

    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies = 1;

    @Column(name = "borrowed_count", nullable = false)
    private Integer borrowedCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CirculationPolicy getCirculationPolicy() {
        return circulationPolicy == null ? CirculationPolicy.AUTO : circulationPolicy;
    }

    public enum CirculationPolicy {
        AUTO,
        MANUAL,
        REFERENCE_ONLY
    }
}
