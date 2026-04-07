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
@Table(name = "users", indexes = {
    @Index(name = "idx_user_student_id", columnList = "student_id", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 20, unique = true)
    private String studentId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String role = "STUDENT";

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(String studentId, String username, String password) {
        this.studentId = studentId;
        this.username = username;
        this.password = password;
    }

    public void incrementLoginCount() {
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
    }
}
