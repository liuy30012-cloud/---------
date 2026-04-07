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
@Table(name = "notification_records", indexes = {
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_notification_user_read", columnList = "user_id, read")
})
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 50)
    private String type;

    @Column(length = 50)
    private String icon;

    @Column(name = "title_zh", nullable = false, length = 120)
    private String titleZh;

    @Column(name = "title_en", nullable = false, length = 120)
    private String titleEn;

    @Column(name = "desc_zh", nullable = false, length = 500)
    private String descZh;

    @Column(name = "desc_en", nullable = false, length = 500)
    private String descEn;

    @Column(name = "target_path", length = 500)
    private String targetPath;

    @Column(nullable = false)
    private Boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
