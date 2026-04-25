package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BookRatingStatistics;
import com.library.dto.BookReviewRequest;
import com.library.dto.BookReviewResponse;
import com.library.service.BookReviewService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class BookReviewController {

    private final BookReviewService bookReviewService;
    private final JwtUtil jwtUtil;

    /**
     * 创建书籍评价
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookReviewResponse>> createReview(
            Authentication authentication,
            @Valid @RequestBody BookReviewRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        BookReviewResponse response = bookReviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Review created successfully"));
    }

    /**
     * 更新书籍评价
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<BookReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            Authentication authentication,
            @Valid @RequestBody BookReviewRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        BookReviewResponse response = bookReviewService.updateReview(reviewId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }

    /**
     * 删除书籍评价
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        bookReviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }

    /**
     * 获取书籍的所有评价（分页）
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<List<BookReviewResponse>>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        Page<BookReviewResponse> reviews = bookReviewService.getBookReviews(bookId, page, size, sortBy);
        return ApiResponse.okWithPagination(
            reviews.getContent(),
            (int) reviews.getTotalElements(),
            reviews.getNumber(),
            reviews.getSize(),
            reviews.getTotalPages()
        );
    }

    /**
     * 获取用户的所有评价（分页）
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BookReviewResponse>>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<BookReviewResponse> reviews = bookReviewService.getUserReviews(userId, page, size);
        return ApiResponse.okWithPagination(
            reviews.getContent(),
            (int) reviews.getTotalElements(),
            reviews.getNumber(),
            reviews.getSize(),
            reviews.getTotalPages()
        );
    }

    /**
     * 获取当前用户的所有评价（分页）
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<ApiResponse<List<BookReviewResponse>>> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserIdFromAuth(authentication);
        Page<BookReviewResponse> reviews = bookReviewService.getUserReviews(userId, page, size);
        return ApiResponse.okWithPagination(
            reviews.getContent(),
            (int) reviews.getTotalElements(),
            reviews.getNumber(),
            reviews.getSize(),
            reviews.getTotalPages()
        );
    }

    /**
     * 获取单个评价详情
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<BookReviewResponse>> getReviewById(@PathVariable Long reviewId) {
        BookReviewResponse review = bookReviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review, "Review retrieved successfully"));
    }

    /**
     * 获取书籍评分统计
     */
    @GetMapping("/book/{bookId}/statistics")
    public ResponseEntity<ApiResponse<BookRatingStatistics>> getBookRatingStatistics(@PathVariable Long bookId) {
        BookRatingStatistics statistics = bookReviewService.getBookRatingStatistics(bookId);
        return ResponseEntity.ok(ApiResponse.success(statistics, "Rating statistics retrieved successfully"));
    }

    /**
     * 获取书籍的最新评价
     */
    @GetMapping("/book/{bookId}/latest")
    public ResponseEntity<ApiResponse<List<BookReviewResponse>>> getLatestReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "5") int limit) {
        List<BookReviewResponse> reviews = bookReviewService.getLatestReviews(bookId, limit);
        return ResponseEntity.ok(ApiResponse.success(reviews, "Latest reviews retrieved successfully"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
