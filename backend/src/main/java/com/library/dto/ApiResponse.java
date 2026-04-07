package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 统一API响应格式
 * 用于消除Controller中重复的Map构建代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Integer total;  // 用于分页查询
    private Integer page;   // 当前页码
    private Integer size;   // 每页大小
    private Integer totalPages; // 总页数

    // 基础构造器
    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    // 成功响应（仅数据）
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, null));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 成功响应（数据 + 消息）
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, message));
    }

    // 成功响应（分页数据）
    public static <T> ResponseEntity<ApiResponse<T>> okWithPagination(
            T data, int total, int page, int size, int totalPages) {
        ApiResponse<T> response = new ApiResponse<>(true, data, null);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(totalPages);
        return ResponseEntity.ok(response);
    }

    // 错误响应（默认400）
    public static <T> ResponseEntity<ApiResponse<T>> error(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, null, message));
    }

    // 错误响应（自定义状态码）
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, null, message));
    }

    // 404响应
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, null, message));
    }

    // 未授权响应
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, null, message));
    }
}
