package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.FavoriteRequest;
import com.library.dto.FavoriteResponse;
import com.library.service.BookFavoriteService;
import com.library.util.JwtUtil;
import com.library.util.PageableHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class BookFavoriteController {

    private final BookFavoriteService bookFavoriteService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            Authentication authentication,
            @Valid @RequestBody FavoriteRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        FavoriteResponse response = bookFavoriteService.addFavorite(userId, request.getBookId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "收藏成功"));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            Authentication authentication,
            @PathVariable Long bookId) {
        Long userId = getUserIdFromAuth(authentication);
        bookFavoriteService.removeFavorite(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(null, "已取消收藏"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getUserFavorites(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<FavoriteResponse> favorites = bookFavoriteService.getUserFavorites(userId, pageable);
        return ApiResponse.okWithPagination(
            favorites.getContent(),
            (int) favorites.getTotalElements(),
            favorites.getNumber(),
            favorites.getSize(),
            favorites.getTotalPages()
        );
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            Authentication authentication,
            @RequestParam Long bookId) {
        Long userId = getUserIdFromAuth(authentication);
        boolean isFavorited = bookFavoriteService.isFavorited(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(isFavorited, "查询收藏状态成功"));
    }

    @PostMapping("/batch-check")
    public ResponseEntity<ApiResponse<Set<Long>>> batchCheckFavorites(
            Authentication authentication,
            @RequestBody List<Long> bookIds) {
        Long userId = getUserIdFromAuth(authentication);
        Set<Long> favoritedIds = bookFavoriteService.batchCheckFavorites(userId, bookIds);
        return ResponseEntity.ok(ApiResponse.success(favoritedIds, "批量查询收藏状态成功"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
