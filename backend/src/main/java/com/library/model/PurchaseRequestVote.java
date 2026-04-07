package com.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "purchase_request_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_purchase_request_vote_request_user", columnNames = {"purchase_request_id", "user_id"})
}, indexes = {
    @Index(name = "idx_purchase_request_vote_request", columnList = "purchase_request_id"),
    @Index(name = "idx_purchase_request_vote_user", columnList = "user_id")
})
public class PurchaseRequestVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_request_id", nullable = false)
    private Long purchaseRequestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
