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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "login_logs", indexes = {
    @Index(name = "idx_login_log_student_time", columnList = "student_id, login_time"),
    @Index(name = "idx_login_log_user_time", columnList = "user_id, login_time")
})
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @CreatedDate
    @Column(name = "login_time", nullable = false, updatable = false)
    private LocalDateTime loginTime;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "login_status", nullable = false)
    private Integer loginStatus;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    public LoginLog() {
    }

    public LoginLog(Long userId, String studentId, String ipAddress,
                    String userAgent, Integer loginStatus, String failReason) {
        this.userId = userId;
        this.studentId = studentId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginStatus = loginStatus;
        this.failReason = failReason;
    }
}
