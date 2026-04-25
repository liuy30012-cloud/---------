package com.library.controller;

import com.library.service.IpBanService;
import com.library.service.SecurityMetricsService;
import com.library.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
public class HoneypotController {

    private final IpBanService ipBanService;
    private final SecurityMetricsService securityMetricsService;

    @Value("${anti-crawler.honeypot.enabled:true}")
    private boolean honeypotEnabled;

    @Value("${anti-crawler.honeypot.ban-duration:3600}")
    private int banDuration;

    @Value("${anti-crawler.honeypot.dry-run:false}")
    private boolean honeypotDryRun;

    public HoneypotController(IpBanService ipBanService, SecurityMetricsService securityMetricsService) {
        this.ipBanService = ipBanService;
        this.securityMetricsService = securityMetricsService;
    }

    @GetMapping("/api/admin/data-export")
    public ResponseEntity<?> fakeDataExport(HttpServletRequest request) {
        return handleHoneypot(request, "/api/admin/data-export");
    }

    @GetMapping("/api/v2/books/all")
    public ResponseEntity<?> fakeAllBooks(HttpServletRequest request) {
        return handleHoneypot(request, "/api/v2/books/all");
    }

    @GetMapping("/api/internal/users")
    public ResponseEntity<?> fakeUserList(HttpServletRequest request) {
        return handleHoneypot(request, "/api/internal/users");
    }

    @GetMapping("/api/admin/db-backup")
    public ResponseEntity<?> fakeDbBackup(HttpServletRequest request) {
        return handleHoneypot(request, "/api/admin/db-backup");
    }

    @GetMapping("/api/swagger.json")
    public ResponseEntity<?> fakeSwagger(HttpServletRequest request) {
        return handleHoneypot(request, "/api/swagger.json");
    }

    @RequestMapping(value = "/graphql", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> fakeGraphql(HttpServletRequest request) {
        return handleHoneypot(request, "/graphql");
    }

    private ResponseEntity<?> handleHoneypot(HttpServletRequest request, String endpoint) {
        String clientIp = ClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        long ipViolations = ipBanService.getViolationCount(clientIp);
        int trackedViolations = ipBanService.getTrackedViolationCount();
        int bannedIps = ipBanService.getBannedIpCount();
        log.warn(
                "Honeypot triggered: ip={} ua={} endpoint={} enabled={} dryRun={} ipViolations={} trackedViolations={} bannedIpCount={}",
                clientIp,
                userAgent,
                endpoint,
                honeypotEnabled,
                honeypotDryRun,
                ipViolations,
                trackedViolations,
                bannedIps
        );
        securityMetricsService.recordHoneypot(endpoint);

        if (honeypotEnabled && !honeypotDryRun) {
            ipBanService.banIp(clientIp, banDuration, "honeypot");
            log.warn("Honeypot banned client ip={} durationSeconds={}", clientIp, banDuration);
        } else if (honeypotEnabled) {
            log.info("Honeypot dry-run enabled, skip banning ip={}", clientIp);
        }

        return ResponseEntity.ok(generateFakeResponse(endpoint));
    }

    private Map<String, Object> generateFakeResponse(String endpoint) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());

        if (endpoint.contains("books")) {
            response.put("data", generateFakeBooks());
            response.put("total", ThreadLocalRandom.current().nextInt(100, 500));
        } else if (endpoint.contains("users")) {
            response.put("data", generateFakeUsers());
            response.put("total", ThreadLocalRandom.current().nextInt(50, 200));
        } else if (endpoint.contains("swagger")) {
            response.put("openapi", "3.0.0");
            response.put("info", Map.of("title", "Library API", "version", "2.0.0"));
            response.put("paths", Map.of());
        } else {
            response.put("data", "Processing...");
            response.put("status", "queued");
        }

        return response;
    }

    private List<Map<String, Object>> generateFakeBooks() {
        List<Map<String, Object>> books = new ArrayList<>();
        String[] titles = {"高等数学", "线性代数", "概率论", "数据结构", "计算机网络"};
        String[] authors = {"张华", "李明", "王强", "赵芳", "陈大"};

        for (int i = 0; i < 5; i++) {
            Map<String, Object> book = new HashMap<>();
            book.put("id", ThreadLocalRandom.current().nextLong(10000, 99999));
            book.put("title", titles[i]);
            book.put("author", authors[i]);
            book.put("isbn", "978-" + ThreadLocalRandom.current().nextInt(1000000, 9999999));
            book.put("location", "A区" + (i + 1) + "层" + ThreadLocalRandom.current().nextInt(1, 20) + "架");
            books.add(book);
        }
        return books;
    }

    private List<Map<String, Object>> generateFakeUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", ThreadLocalRandom.current().nextLong(10000, 99999));
            user.put("studentId", "20" + ThreadLocalRandom.current().nextInt(10, 26)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999)));
            user.put("name", "用户" + ThreadLocalRandom.current().nextInt(1, 100));
            user.put("email", "fake" + i + "@example.com");
            users.add(user);
        }
        return users;
    }
}
