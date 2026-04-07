package com.library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return fieldError.getDefaultMessage();
                }
                return error.getDefaultMessage();
            })
            .collect(Collectors.joining("; "));

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("业务参数异常: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", sanitizeErrorMessage(ex.getMessage()));

        return ResponseEntity.badRequest().body(response);
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "操作失败";
        }
        if (message.contains("SQL") || message.contains("Exception")
            || message.contains("at ") || message.contains("\\") || message.contains("/")) {
            return "操作失败，请稍后重试";
        }
        return message;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimitExceededException(RateLimitExceededException ex) {
        log.warn("频率限制: IP={}, 消息={}", ex.getClientIp(), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        response.put("retryAfter", ex.getRetryAfterSeconds());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(response);
    }

    @ExceptionHandler(CrawlerDetectedException.class)
    public ResponseEntity<?> handleCrawlerDetectedException(CrawlerDetectedException ex) {
        log.warn("爬虫检测: IP={}, 原因={}", ex.getClientIp(), ex.getReason());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "请求被拒绝");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("未授权访问: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "权限不足");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("资源未找到: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("服务器内部错误", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "服务器内部错误，请稍后重试");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
