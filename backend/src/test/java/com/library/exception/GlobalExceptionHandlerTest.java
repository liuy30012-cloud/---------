package com.library.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentExceptionSanitizesStackTraceLeak() {
        ResponseEntity<?> response = handler.handleIllegalArgumentException(
            new IllegalArgumentException("at com.library.SomeClass.method(SomeClass.java:42)")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("操作失败，请稍后重试", body.get("message"));
    }

    @Test
    void illegalArgumentExceptionPreservesNormalBusinessMessage() {
        ResponseEntity<?> response = handler.handleIllegalArgumentException(
            new IllegalArgumentException("图书不存在")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("图书不存在", body.get("message"));
    }

    @Test
    void genericExceptionReturnsChineseFallbackMessage() {
        ResponseEntity<?> response = handler.handleGenericException(new RuntimeException("boom"));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("服务器内部错误，请稍后重试", body.get("message"));
    }
}
