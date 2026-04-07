package com.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "book_reviews", indexes = {
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_rating", columnList = "rating"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于 1 分")
    @Max(value = 5, message = "评分不能高于 5 分")
    @Column(nullable = false)
    private Integer rating;

    @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
