package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.FavoriteRequest;
import com.library.dto.FavoriteResponse;
import com.library.service.BookFavoriteService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody FavoriteRequest request) {
        Long userId = extractUserId(token);
        FavoriteResponse response = bookFavoriteService.addFavorite(userId, request.getBookId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "收藏成功"));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        Long userId = extractUserId(token);
        bookFavoriteService.removeFavorite(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(null, "已取消收藏"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getUserFavorites(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        List<FavoriteResponse> favorites = bookFavoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(ApiResponse.success(favorites, "获取收藏列表成功"));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @RequestHeader("Authorization") String token,
            @RequestParam Long bookId) {
        Long userId = extractUserId(token);
        boolean isFavorited = bookFavoriteService.isFavorited(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(isFavorited, "查询收藏状态成功"));
    }

    @PostMapping("/batch-check")
    public ResponseEntity<ApiResponse<Set<Long>>> batchCheckFavorites(
            @RequestHeader("Authorization") String token,
            @RequestBody List<Long> bookIds) {
        Long userId = extractUserId(token);
        Set<Long> favoritedIds = bookFavoriteService.batchCheckFavorites(userId, bookIds);
        return ResponseEntity.ok(ApiResponse.success(favoritedIds, "批量查询收藏状态成功"));
    }

    private Long extractUserId(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(jwt);
    }
}
