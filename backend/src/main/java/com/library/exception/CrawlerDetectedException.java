package com.library.exception;

/**
 * 爬虫检测异常
 */
public class CrawlerDetectedException extends RuntimeException {

    private final String clientIp;
    private final String reason;

    public CrawlerDetectedException(String message, String clientIp, String reason) {
        super(message);
        this.clientIp = clientIp;
        this.reason = reason;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getReason() {
        return reason;
    }
}
