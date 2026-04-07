package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "borrow_records", indexes = {
    @Index(name = "idx_borrow_record_status_due_date", columnList = "status, due_date"),
    @Index(name = "idx_borrow_record_user_status", columnList = "user_id, status"),
    @Index(name = "idx_borrow_record_book_status", columnList = "book_id, status"),
    @Index(name = "idx_borrow_record_created_at", columnList = "created_at"),
    @Index(name = "idx_borrow_record_borrow_date", columnList = "borrow_date"),
    @Index(name = "idx_borrow_record_return_date", columnList = "return_date")
})
public class BorrowRecord {

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

    @Column(name = "book_isbn", length = 50)
    private String bookIsbn;

    // 借阅信息
    @Column(name = "borrow_date")
    private LocalDateTime borrowDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    // 状态管理
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status;

    // 续借信息
    @Column(nullable = false)
    private Boolean renewed = false;

    @Column(name = "renew_count", nullable = false)
    private Integer renewCount = 0;

    // 逾期信息
    @Column(name = "overdue_days", nullable = false)
    private Integer overdueDays = 0;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "fine_paid", nullable = false)
    private Boolean finePaid = false;

    // 审核信息
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "pickup_deadline")
    private LocalDateTime pickupDeadline;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // 备注
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 借阅状态枚举
    public enum BorrowStatus {
        PENDING,    // 待审核
        APPROVED,   // 已批准
        BORROWED,   // 借阅中
        RETURNED,   // 已归还
        OVERDUE,    // 已逾期
        REJECTED    // 已拒绝
    }
}
