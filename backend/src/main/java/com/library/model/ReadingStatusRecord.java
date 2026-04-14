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
@Table(name = "reading_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_book_reading", columnNames = {"user_id", "book_id"})
}, indexes = {
        @Index(name = "idx_reading_user_id", columnList = "user_id"),
        @Index(name = "idx_reading_book_id", columnList = "book_id"),
        @Index(name = "idx_reading_user_status", columnList = "user_id, status")
})
public class ReadingStatusRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
