package com.library.service;

import com.library.security.SecurityStateStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RequestPatternAnalyzer {

    private static final int MAX_HISTORY_PER_IP = 120;
    private static final int ANALYSIS_WINDOW_MS = 60_000;
    private static final Duration SUSPICION_TTL = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Deque<RequestRecord>> requestHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> ipFingerprints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> fingerprintIps = new ConcurrentHashMap<>();

    private final SecurityStateStore stateStore;
    private final Clock clock;
    private ScheduledExecutorService cleanupExecutor;

    @Value("${anti-crawler.challenge.suspicion-threshold:70}")
    private int suspicionThreshold;

    @Value("${anti-crawler.challenge.cooldown-base-seconds:30}")
    private int cooldownBaseSeconds;

    @Value("${anti-crawler.challenge.cooldown-max-seconds:120}")
    private int cooldownMaxSeconds;

    public RequestPatternAnalyzer(SecurityStateStore stateStore, Clock clock) {
        this.stateStore = stateStore;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "pattern-analyzer-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 3, 3, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    public int recordAndAnalyze(String clientIp, String path, String fingerprint) {
        String normalizedIp = normalize(clientIp);
        long now = clock.millis();

        Deque<RequestRecord> history = requestHistory.computeIfAbsent(normalizedIp, ignored -> new ConcurrentLinkedDeque<>());
        history.addLast(new RequestRecord(path, now));
        while (history.size() > MAX_HISTORY_PER_IP) {
            history.pollFirst();
        }

        if (fingerprint != null && !fingerprint.isBlank()) {
            ipFingerprints.computeIfAbsent(normalizedIp, ignored -> ConcurrentHashMap.newKeySet()).add(fingerprint);
            fingerprintIps.computeIfAbsent(fingerprint, ignored -> ConcurrentHashMap.newKeySet()).add(normalizedIp);
        }

        int patternScore = analyzePatterns(history, now);
        int fingerprintScore = calculateFingerprintScore(normalizedIp, fingerprint);
        int currentScore = getSuspicionScore(normalizedIp);
        int nextScore = Math.min(100, (int) Math.round(currentScore * 0.65 + (patternScore + fingerprintScore) * 0.35));
        setSuspicionScore(normalizedIp, nextScore);

        if (nextScore >= suspicionThreshold) {
            armCooldown(normalizedIp, nextScore);
        }

        return nextScore;
    }

    public int analyzeFingerprint(String clientIp, String fingerprint) {
        return calculateFingerprintScore(normalize(clientIp), fingerprint);
    }

    public void recordCaptchaFailure(String clientIp, String fingerprint) {
        String normalizedIp = normalize(clientIp);
        int penalty = 25 + calculateFingerprintScore(normalizedIp, fingerprint) / 2;
        int nextScore = Math.min(100, getSuspicionScore(normalizedIp) + penalty);
        setSuspicionScore(normalizedIp, nextScore);
        armCooldown(normalizedIp, nextScore);
    }

    public boolean hasActiveCooldown(String clientIp) {
        return stateStore.get(cooldownKey(normalize(clientIp))) != null;
    }

    public int getCooldownRemainingSeconds(String clientIp) {
        return (int) stateStore.getRemainingTtlSeconds(cooldownKey(normalize(clientIp)));
    }

    public int getSuspicionScore(String clientIp) {
        String value = stateStore.get(suspicionKey(normalize(clientIp)));
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public void reset() {
        requestHistory.clear();
        ipFingerprints.clear();
        fingerprintIps.clear();
    }

    private int analyzePatterns(Deque<RequestRecord> history, long now) {
        List<RequestRecord> recent = new ArrayList<>();
        for (RequestRecord record : history) {
            if (now - record.timestamp < ANALYSIS_WINDOW_MS) {
                recent.add(record);
            }
        }

        if (recent.size() < 5) {
            return 0;
        }

        int total = 0;
        total += detectSequentialAccess(recent);
        total += detectRegularInterval(recent);
        total += detectBreadthCrawling(recent);
        return Math.min(total, 100);
    }

    private int detectSequentialAccess(List<RequestRecord> records) {
        int sequentialCount = 0;
        long lastId = -1L;

        for (RequestRecord record : records) {
            long id = extractNumericId(record.path);
            if (id >= 0 && lastId >= 0 && id == lastId + 1) {
                sequentialCount++;
            }
            if (id >= 0) {
                lastId = id;
            }
        }

        if (sequentialCount >= 8) {
            return 45;
        }
        if (sequentialCount >= 5) {
            return 25;
        }
        if (sequentialCount >= 3) {
            return 10;
        }
        return 0;
    }

    private int detectRegularInterval(List<RequestRecord> records) {
        if (records.size() < 5) {
            return 0;
        }

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            intervals.add(records.get(i).timestamp - records.get(i - 1).timestamp);
        }

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0D);
        if (mean < 50D) {
            return 0;
        }

        double variance = intervals.stream()
                .mapToDouble(interval -> Math.pow(interval - mean, 2))
                .average()
                .orElse(0D);
        double coefficientOfVariation = Math.sqrt(variance) / mean;

        if (coefficientOfVariation < 0.05D) {
            return 30;
        }
        if (coefficientOfVariation < 0.1D) {
            return 18;
        }
        if (coefficientOfVariation < 0.2D) {
            return 8;
        }
        return 0;
    }

    private int detectBreadthCrawling(List<RequestRecord> records) {
        Set<String> uniquePaths = new HashSet<>();
        for (RequestRecord record : records) {
            uniquePaths.add(record.path.replaceAll("/\\d+", "/{id}"));
        }

        if (uniquePaths.size() > 20) {
            return 25;
        }
        if (uniquePaths.size() > 15) {
            return 12;
        }
        return 0;
    }

    private int calculateFingerprintScore(String clientIp, String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 8;
        }

        int score = 0;
        Set<String> fingerprintsForIp = ipFingerprints.get(clientIp);
        if (fingerprintsForIp != null && fingerprintsForIp.size() > 2) {
            score += Math.min((fingerprintsForIp.size() - 2) * 12, 36);
        }

        Set<String> ipsForFingerprint = fingerprintIps.get(fingerprint);
        if (ipsForFingerprint != null && ipsForFingerprint.size() > 4) {
            score += Math.min((ipsForFingerprint.size() - 4) * 8, 24);
        }

        return score;
    }

    private long extractNumericId(String path) {
        if (path == null || path.isBlank()) {
            return -1L;
        }

        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException ignored) {
                // ignore and continue
            }
        }
        return -1L;
    }

    private void setSuspicionScore(String clientIp, int score) {
        stateStore.set(suspicionKey(clientIp), String.valueOf(Math.max(score, 0)), SUSPICION_TTL);
    }

    private void armCooldown(String clientIp, int suspicionScore) {
        int cooldownSeconds = Math.min(
                cooldownBaseSeconds + Math.max(suspicionScore - suspicionThreshold, 0) * 2,
                cooldownMaxSeconds
        );
        stateStore.set(cooldownKey(clientIp), String.valueOf(clock.millis() + cooldownSeconds * 1000L), Duration.ofSeconds(cooldownSeconds));
        log.warn("Activated bot cooldown: ip={} suspicionScore={} cooldownSeconds={}", clientIp, suspicionScore, cooldownSeconds);
    }

    private String suspicionKey(String clientIp) {
        return "security:bot:suspicion:" + clientIp;
    }

    private String cooldownKey(String clientIp) {
        return "security:bot:cooldown:" + clientIp;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private void cleanup() {
        long cutoff = clock.millis() - ANALYSIS_WINDOW_MS * 3L;
        requestHistory.forEach((ip, history) -> {
            while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
                history.pollFirst();
            }
        });
        requestHistory.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ipFingerprints.entrySet().removeIf(entry -> entry.getValue().isEmpty() || !requestHistory.containsKey(entry.getKey()));
        fingerprintIps.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private record RequestRecord(String path, long timestamp) {
    }
}
