package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.SearchHistoryRequest;
import com.library.model.SearchHistoryRecord;
import com.library.service.SearchHistoryService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchHistoryRecord>>> getHistory(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<SearchHistoryRecord> history = searchHistoryService.getHistory(userId);
        return ApiResponse.ok(history);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SearchHistoryRecord>> addHistory(@Valid @RequestBody SearchHistoryRequest body, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        SearchHistoryRecord record = searchHistoryService.saveSearch(userId, body);
        return ApiResponse.ok(record);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearHistory(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        searchHistoryService.clearHistory(userId);
        return ApiResponse.ok(null, "搜索历史已清空");
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }

        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
