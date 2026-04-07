package com.library.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.service.AntiCrawlerRequestInspector;
import com.library.util.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class AntiCrawlerFilter extends OncePerRequestFilter {

    private final AntiCrawlerRequestInspector antiCrawlerRequestInspector;
    private final ObjectMapper objectMapper;

    public AntiCrawlerFilter(AntiCrawlerRequestInspector antiCrawlerRequestInspector, ObjectMapper objectMapper) {
        this.antiCrawlerRequestInspector = antiCrawlerRequestInspector;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        AntiCrawlerRequestInspector.Decision decision = antiCrawlerRequestInspector.inspect(
            request.getRequestURI(),
            ClientIpResolver.resolve(request),
            request.getHeader("User-Agent"),
            request.getHeader("Accept"),
            request.getHeader("X-Request-Sign"),
            request.getHeader("X-Request-Timestamp"),
            request.getHeader("X-Request-Nonce")
        );

        if (!decision.allowed()) {
            sendForbiddenResponse(response, decision.message(), decision.code());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message, String code) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
            "success", false,
            "message", message,
            "code", code
        )));
    }
}
