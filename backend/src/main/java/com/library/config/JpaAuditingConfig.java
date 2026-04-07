package com.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @org.springframework.context.annotation.Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
