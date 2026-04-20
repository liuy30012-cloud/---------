package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.SearchHistoryRequest;
import com.library.model.SearchHistoryRecord;
import com.library.service.SearchHistoryService;
import com.library.util.JwtUtil;
import com.library.util.PageableHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SearchHistoryRecord>>> getHistory(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size,
        @RequestParam(defaultValue = "timestamp,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 30, sort);
        Page<SearchHistoryRecord> history = searchHistoryService.getHistory(userId, pageable);
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
