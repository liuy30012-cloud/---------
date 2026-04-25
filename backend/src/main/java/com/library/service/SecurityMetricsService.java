package com.library.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SecurityMetricsService {

    private final MeterRegistry meterRegistry;

    public SecurityMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRateLimitBlocked(String route, String code) {
        meterRegistry.counter(
                "library.security.rate_limit.blocked",
                "route", route,
                "code", code
        ).increment();
    }

    public void recordCaptcha(String stage, String result) {
        meterRegistry.counter(
                "library.security.captcha",
                "stage", stage,
                "result", result
        ).increment();
    }

    public void recordIpBan(String source) {
        meterRegistry.counter(
                "library.security.ip_ban",
                "source", source
        ).increment();
    }

    public void recordHoneypot(String endpoint) {
        meterRegistry.counter(
                "library.security.honeypot",
                "endpoint", endpoint
        ).increment();
    }

    public void recordBotCooldown() {
        meterRegistry.counter("library.security.bot_cooldown").increment();
    }
}
