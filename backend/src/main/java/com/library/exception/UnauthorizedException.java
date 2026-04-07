package com.library.exception;

/**
 * 未认证异常
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
