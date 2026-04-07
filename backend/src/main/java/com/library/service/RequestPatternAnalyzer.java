package com.library.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求行为模式分析器
 * 
 * 检测爬虫的行为特征：
 * 1. 顺序遍历检测 — 连续访问 /api/books/1, /api/books/2, /api/books/3...
 * 2. 规律间隔检测 — 请求间隔高度一致（如固定每2秒一次）
 * 3. 路径多样性检测 — 短时间内访问大量不同路径（广度爬取）
 * 4. 设备指纹一致性 — 同IP不同指纹（代理池特征）或同指纹不同IP（指纹伪造）
 * 5. 渐进式限速 — 对可疑IP逐步增加响应延迟
 */
@Slf4j
@Service
public class RequestPatternAnalyzer {

    // IP -> 最近请求路径时间序列
    private final ConcurrentHashMap<String, Deque<RequestRecord>> requestHistory = new ConcurrentHashMap<>();
    
    // IP -> 可疑程度分数 (0-100, 越高越可疑)
    private final ConcurrentHashMap<String, AtomicInteger> suspicionScores = new ConcurrentHashMap<>();

    // IP -> 设备指纹集合
    private final ConcurrentHashMap<String, Set<String>> ipFingerprints = new ConcurrentHashMap<>();

    // 设备指纹 -> IP 集合
    private final ConcurrentHashMap<String, Set<String>> fingerprintIps = new ConcurrentHashMap<>();

    // IP -> 渐进延迟毫秒数
    private final ConcurrentHashMap<String, Integer> progressiveDelays = new ConcurrentHashMap<>();
    private final Clock clock;

    private static final int MAX_HISTORY_PER_IP = 100;
    private static final int ANALYSIS_WINDOW_MS = 60_000; // 1分钟分析窗口
    private static final int SUSPICION_THRESHOLD = 50; // 触发告警的分数阈值
    private static final int MAX_PROGRESSIVE_DELAY_MS = 5000; // 最大渐进延迟

    private ScheduledExecutorService cleanupExecutor;

    public RequestPatternAnalyzer(Clock clock) {
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pattern-analyzer-cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每3分钟清理一次
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 3, 3, TimeUnit.MINUTES);
        log.info("反爬虫: 请求模式分析器已启动");
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    /**
     * 记录请求并分析行为模式
     * 
     * @return 可疑程度分数 (0-100)
     */
    public int recordAndAnalyze(String clientIp, String path, String fingerprint) {
        long now = clock.millis();

        // 记录请求
        Deque<RequestRecord> history = requestHistory.computeIfAbsent(clientIp, 
                k -> new ConcurrentLinkedDeque<>());
        history.addLast(new RequestRecord(path, now));

        // 限制历史记录大小
        while (history.size() > MAX_HISTORY_PER_IP) {
            history.pollFirst();
        }

        // 记录设备指纹映射
        if (fingerprint != null && !fingerprint.isEmpty()) {
            ipFingerprints.computeIfAbsent(clientIp, k -> ConcurrentHashMap.newKeySet())
                    .add(fingerprint);
            fingerprintIps.computeIfAbsent(fingerprint, k -> ConcurrentHashMap.newKeySet())
                    .add(clientIp);
        }

        // 执行行为分析
        int score = analyzePatterns(clientIp, history, now);

        // 更新可疑分数（指数移动平均）
        AtomicInteger currentScore = suspicionScores.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int oldScore = currentScore.get();
        int newScore = (int) (oldScore * 0.7 + score * 0.3);
        currentScore.set(newScore);

        // 更新渐进延迟
        if (newScore > SUSPICION_THRESHOLD) {
            int delay = Math.min((newScore - SUSPICION_THRESHOLD) * 100, MAX_PROGRESSIVE_DELAY_MS);
            progressiveDelays.put(clientIp, delay);
            log.warn("反爬虫-模式分析: IP [{}] 可疑分数={}, 延迟={}ms", clientIp, newScore, delay);
        } else {
            progressiveDelays.remove(clientIp);
        }

        return newScore;
    }

    /**
     * 获取IP应施加的渐进延迟（毫秒）
     */
    public int getProgressiveDelay(String clientIp) {
        return progressiveDelays.getOrDefault(clientIp, 0);
    }

    /**
     * 获取IP的可疑分数
     */
    public int getSuspicionScore(String clientIp) {
        AtomicInteger score = suspicionScores.get(clientIp);
        return score != null ? score.get() : 0;
    }

    /**
     * 检查设备指纹是否异常
     * - 同IP使用多个不同指纹 → 可能是指纹轮换
     * - 同指纹在多个IP上出现 → 可能是代理池
     */
    public int analyzeFingerprint(String clientIp, String fingerprint) {
        int score = 0;

        if (fingerprint == null || fingerprint.isEmpty()) {
            return 10; // 无指纹，轻度可疑
        }

        // 同IP不同指纹数量
        Set<String> fps = ipFingerprints.get(clientIp);
        if (fps != null && fps.size() > 3) {
            score += Math.min((fps.size() - 3) * 15, 40);
            log.debug("反爬虫-指纹分析: IP [{}] 使用了{}个不同指纹", clientIp, fps.size());
        }

        // 同指纹不同IP数量
        Set<String> ips = fingerprintIps.get(fingerprint);
        if (ips != null && ips.size() > 5) {
            score += Math.min((ips.size() - 5) * 10, 30);
            log.debug("反爬虫-指纹分析: 指纹 [{}...] 出现在{}个IP上", 
                    fingerprint.substring(0, Math.min(8, fingerprint.length())), ips.size());
        }

        return score;
    }

    public void reset() {
        requestHistory.clear();
        suspicionScores.clear();
        ipFingerprints.clear();
        fingerprintIps.clear();
        progressiveDelays.clear();
    }

    /**
     * 核心分析方法
     */
    private int analyzePatterns(String clientIp, Deque<RequestRecord> history, long now) {
        List<RequestRecord> recent = new ArrayList<>();
        for (RequestRecord r : history) {
            if (now - r.timestamp < ANALYSIS_WINDOW_MS) {
                recent.add(r);
            }
        }

        if (recent.size() < 5) {
            return 0; // 请求量太少，无法判断
        }

        int totalScore = 0;

        // 1. 顺序遍历检测
        totalScore += detectSequentialAccess(recent);

        // 2. 规律间隔检测
        totalScore += detectRegularInterval(recent);

        // 3. 路径多样性（广度爬取）
        totalScore += detectBreadthCrawling(recent);

        return Math.min(totalScore, 100);
    }

    /**
     * 检测顺序遍历 — /api/books/1 -> /api/books/2 -> /api/books/3
     */
    private int detectSequentialAccess(List<RequestRecord> records) {
        int sequentialCount = 0;
        long lastId = -1;

        for (RequestRecord r : records) {
            // 提取路径中的数字ID
            long id = extractNumericId(r.path);
            if (id >= 0 && lastId >= 0) {
                if (id == lastId + 1) {
                    sequentialCount++;
                }
            }
            if (id >= 0) {
                lastId = id;
            }
        }

        if (sequentialCount >= 8) {
            return 40; // 高度可疑
        } else if (sequentialCount >= 5) {
            return 25;
        } else if (sequentialCount >= 3) {
            return 10;
        }
        return 0;
    }

    /**
     * 检测规律间隔 — 请求间隔变异系数(CV)极小
     */
    private int detectRegularInterval(List<RequestRecord> records) {
        if (records.size() < 5) return 0;

        // 计算相邻请求间隔
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            intervals.add(records.get(i).timestamp - records.get(i - 1).timestamp);
        }

        // 计算平均值和标准差
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean < 50) return 0; // 间隔太短无法判断

        double variance = intervals.stream()
                .mapToDouble(i -> Math.pow(i - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // 变异系数 = 标准差 / 平均值
        double cv = stdDev / mean;

        // CV < 0.1 表示高度规律（几乎固定间隔，典型爬虫行为）
        if (cv < 0.05) {
            return 35; // 极度规律
        } else if (cv < 0.1) {
            return 20; // 高度规律
        } else if (cv < 0.2) {
            return 8; // 偏规律
        }
        return 0;
    }

    /**
     * 检测广度爬取 — 短时间内访问大量不同路径
     */
    private int detectBreadthCrawling(List<RequestRecord> records) {
        Set<String> uniquePaths = new HashSet<>();
        for (RequestRecord r : records) {
            // 归一化路径（去掉数字ID）
            String normalizedPath = r.path.replaceAll("/\\d+", "/{id}");
            uniquePaths.add(normalizedPath);
        }

        // 1分钟内访问超过15个不同路径模式
        if (uniquePaths.size() > 20) {
            return 30;
        } else if (uniquePaths.size() > 15) {
            return 15;
        }
        return 0;
    }

    /**
     * 从路径中提取数字ID
     */
    private long extractNumericId(String path) {
        // 匹配 /api/books/123 形式
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException e) {
                // 继续
            }
        }
        return -1;
    }

    /**
     * 清理过期数据
     */
    private void cleanup() {
        long now = clock.millis();
        long cutoff = now - ANALYSIS_WINDOW_MS * 3; // 保留3个窗口的数据

        // 清理过期请求历史
        requestHistory.forEach((ip, history) -> {
            while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
                history.pollFirst();
            }
        });
        requestHistory.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // 清理过期分数
        suspicionScores.entrySet().removeIf(entry -> {
            Deque<RequestRecord> h = requestHistory.get(entry.getKey());
            return h == null || h.isEmpty();
        });

        // 限制指纹映射大小
        ipFingerprints.entrySet().removeIf(entry -> entry.getValue().size() > 50);
        fingerprintIps.entrySet().removeIf(entry -> entry.getValue().size() > 100);

        progressiveDelays.entrySet().removeIf(entry -> !suspicionScores.containsKey(entry.getKey()));

        log.debug("反爬虫-模式分析清理: 跟踪IP={}, 可疑IP={}", 
                requestHistory.size(), 
                suspicionScores.entrySet().stream()
                        .filter(e -> e.getValue().get() > SUSPICION_THRESHOLD).count());
    }

    // 请求记录
    private static class RequestRecord {
        final String path;
        final long timestamp;

        RequestRecord(String path, long timestamp) {
            this.path = path;
            this.timestamp = timestamp;
        }
    }
}
