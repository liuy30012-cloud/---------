package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_tags", indexes = {
    @Index(name = "idx_book_tag", columnList = "book_id,tag_name"),
    @Index(name = "idx_tag_name", columnList = "tag_name")
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class BookTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;

    @Column(name = "tag_type", length = 20)
    private String tagType; // SYSTEM, USER_GENERATED

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
