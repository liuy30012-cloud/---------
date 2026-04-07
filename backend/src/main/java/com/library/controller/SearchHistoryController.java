package com.library.controller;

import com.library.dto.SearchHistoryRequest;
import com.library.model.SearchHistoryRecord;
import com.library.service.SearchHistoryService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<SearchHistoryRecord> getHistory(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return searchHistoryService.getHistory(userId);
    }

    @PostMapping
    public SearchHistoryRecord addHistory(@Valid @RequestBody SearchHistoryRequest body, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return searchHistoryService.saveSearch(userId, body);
    }

    @DeleteMapping
    public Map<String, String> clearHistory(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        searchHistoryService.clearHistory(userId);

        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        return result;
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }

        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
