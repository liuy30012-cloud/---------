package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "book_favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_book_favorite", columnList = "user_id, book_id")
}, indexes = {
        @Index(name = "idx_favorite_user_id", columnList = "user_id"),
        @Index(name = "idx_favorite_book_id", columnList = "book_id")
})
public class BookFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
