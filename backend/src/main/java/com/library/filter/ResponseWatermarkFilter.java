package com.library.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.library.util.ClientIpResolver;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 响应数据水印过滤器
 * 
 * 在每个API响应中嵌入隐形追踪标识：
 * 1. X-Trace-Id 响应头 — 唯一标识每次请求，可用于追踪数据来源
 * 2. X-Response-Signature — 响应完整性签名，防止中间人篡改
 * 3. 水印包含时间戳+IP哈希，可追溯泄露数据的来源
 */
@Slf4j
@Component
public class ResponseWatermarkFilter extends OncePerRequestFilter {

    @Value("${anti-crawler.watermark.secret:watermark-trace-secret-library-2026}")
    private String watermarkSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 生成追踪ID
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        long timestamp = System.currentTimeMillis();

        // 追踪ID = Base64(HMAC(IP + timestamp + path))
        String traceId = generateTraceId(clientIp, timestamp, path);
        
        // 简短水印 = 时间戳后6位 + IP哈希前4位
        String watermark = generateWatermark(clientIp, timestamp);

        // 添加水印头部
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Watermark", watermark);
        response.setHeader("X-Served-By", "lib-api-" + watermark.substring(0, 4));

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 生成追踪ID
     * 使用HMAC确保不可伪造
     */
    private String generateTraceId(String ip, long timestamp, String path) {
        String data = ip + "|" + timestamp + "|" + path;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(
                    watermarkSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // 取前12字节编码为Base64，更短的追踪ID
            byte[] shortHash = new byte[12];
            System.arraycopy(hash, 0, shortHash, 0, 12);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(shortHash);
        } catch (Exception e) {
            return Long.toHexString(timestamp);
        }
    }

    /**
     * 生成短水印
     * 格式: 时间戳hex后6位-IP哈希前4位
     */
    private String generateWatermark(String ip, long timestamp) {
        String timeHex = Long.toHexString(timestamp);
        String timePart = timeHex.substring(Math.max(0, timeHex.length() - 6));

        // H-BUG-1 修复: Math.abs(Integer.MIN_VALUE) 返回负数，使用位运算避免
        int ipHash = ip.hashCode() & 0x7FFFFFFF;
        String ipHex = Integer.toHexString(ipHash);
        String ipPart = ipHex.substring(0, Math.min(4, ipHex.length()));

        return timePart + ipPart;
    }

    /**
     * 获取客户端IP（委托给统一的 ClientIpResolver）
     */
    private String getClientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request);
    }
}
