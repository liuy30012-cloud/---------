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
@Table(name = "reservation_records", indexes = {
    @Index(name = "idx_reservation_record_book_status", columnList = "book_id, status"),
    @Index(name = "idx_reservation_record_status_expire", columnList = "status, expire_date"),
    @Index(name = "idx_reservation_record_user_status", columnList = "user_id, status")
})
public class ReservationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    // 预约信息
    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

    @Column(name = "expire_date")
    private LocalDateTime expireDate;

    @Column(name = "notify_date")
    private LocalDateTime notifyDate;

    // 状态管理
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    // 队列信息
    @Column(name = "queue_position")
    private Integer queuePosition;

    // 取消信息
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    // 延期管理
    @Column(name = "extension_count")
    private Integer extensionCount = 0;

    @Column(name = "extension_date")
    private LocalDateTime extensionDate;

    @Column(name = "original_expire_date")
    private LocalDateTime originalExpireDate;

    // 提醒管理
    @Column(name = "expiry_reminder_sent")
    private Boolean expiryReminderSent = false;

    // 灵活取书窗口
    @Column(name = "pickup_window_days")
    private Integer pickupWindowDays = 3;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 预约状态枚举
    public enum ReservationStatus {
        WAITING,    // 等待中
        AVAILABLE,  // 可取书
        COMPLETED,  // 已完成
        CANCELLED,  // 已取消
        EXPIRED     // 已过期
    }
}
