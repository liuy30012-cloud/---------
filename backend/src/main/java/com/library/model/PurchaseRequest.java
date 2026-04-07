package com.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "purchase_requests", indexes = {
    @Index(name = "idx_purchase_request_status_created", columnList = "status, created_at"),
    @Index(name = "idx_purchase_request_proposer", columnList = "proposer_user_id"),
    @Index(name = "idx_purchase_request_dedupe_key", columnList = "dedupe_key", unique = true)
})
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 32)
    private String isbn;

    @Column(length = 500)
    private String reason;

    @Column(name = "proposer_user_id", nullable = false)
    private Long proposerUserId;

    @Column(name = "support_count", nullable = false)
    private Integer supportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseRequestStatus status = PurchaseRequestStatus.PENDING_REVIEW;

    @Column(name = "status_note", length = 200)
    private String statusNote;

    @Column(name = "dedupe_key", nullable = false, length = 260, unique = true)
    private String dedupeKey;

    @Column(name = "status_managed_manually", nullable = false)
    private Boolean statusManagedManually = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
