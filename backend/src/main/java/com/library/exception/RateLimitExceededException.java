package com.library.exception;

/**
 * 请求频率超限异常
 */
public class RateLimitExceededException extends RuntimeException {

    private final String clientIp;
    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, String clientIp, int retryAfterSeconds) {
        super(message);
        this.clientIp = clientIp;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, String clientIp) {
        this(message, clientIp, 60);
    }

    public String getClientIp() {
        return clientIp;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
