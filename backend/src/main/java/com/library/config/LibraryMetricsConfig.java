package com.library.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus监控配置
 */
@Configuration
public class LibraryMetricsConfig {

    /**
     * 自定义Metrics标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "library-positioning-backend",
                "environment", System.getProperty("spring.profiles.active", "dev")
            );
    }

    /**
     * 注册自定义业务指标
     */
    @Bean
    public CustomMetrics customMetrics(MeterRegistry registry) {
        return new CustomMetrics(registry);
    }

    /**
     * 自定义业务指标类
     */
    public static class CustomMetrics {
        private final MeterRegistry registry;

        public CustomMetrics(MeterRegistry registry) {
            this.registry = registry;
        }

        /**
         * 记录借阅操作
         */
        public void recordBorrowOperation(String operation, String status, long durationMs) {
            Timer.builder("library.borrow.operation")
                .tag("operation", operation)
                .tag("status", status)
                .description("借阅操作耗时")
                .register(registry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        /**
         * 记录登录操作
         */
        public void recordLoginAttempt(String status) {
            registry.counter("library.login.attempts",
                "status", status
            ).increment();
        }

        /**
         * 记录API调用
         */
        public void recordApiCall(String endpoint, String method, int statusCode) {
            registry.counter("library.api.calls",
                "endpoint", endpoint,
                "method", method,
                "status", String.valueOf(statusCode)
            ).increment();
        }

        /**
         * 记录当前借阅数量
         */
        public void recordCurrentBorrows(long count) {
            registry.gauge("library.borrows.current", count);
        }

        /**
         * 记录库存告警
         */
        public void recordInventoryAlert(String bookId, int availableCopies) {
            registry.counter("library.inventory.alerts",
                "book_id", bookId,
                "available", String.valueOf(availableCopies)
            ).increment();
        }
    }
}
