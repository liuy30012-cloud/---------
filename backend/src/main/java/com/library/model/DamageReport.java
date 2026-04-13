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
@Table(name = "damage_reports", indexes = {
    @Index(name = "idx_damage_book", columnList = "book_id"),
    @Index(name = "idx_damage_reporter", columnList = "reporter_id"),
    @Index(name = "idx_damage_status", columnList = "status"),
    @Index(name = "idx_damage_created", columnList = "created_at")
})
public class DamageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reporter_name", nullable = false, length = 50)
    private String reporterName;

    @Column(name = "damage_types", nullable = false, length = 200)
    private String damageTypes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_by_name", length = 50)
    private String resolvedByName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum DamageStatus {
        PENDING,
        IN_PROGRESS,
        RESOLVED,
        REJECTED
    }

    public enum DamageType {
        COVER_TORN,
        PAGE_MISSING,
        WATER_DAMAGE,
        GRAFFITI,
        BINDING_BROKEN,
        OTHER
    }
}
