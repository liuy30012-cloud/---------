package com.library.service;

import com.library.dto.SearchHistoryRequest;
import com.library.model.SearchHistoryRecord;
import com.library.repository.SearchHistoryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final int MAX_RECENT_HISTORY = 20;

    private final SearchHistoryRecordRepository searchHistoryRecordRepository;

    public List<SearchHistoryRecord> getHistory(Long userId) {
        return searchHistoryRecordRepository.findByUserIdOrderBySavedDescTimestampDesc(userId);
    }

    public Page<SearchHistoryRecord> getHistory(Long userId, Pageable pageable) {
        return searchHistoryRecordRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Transactional
    public SearchHistoryRecord saveSearch(Long userId, SearchHistoryRequest request) {
        String keyword = request.getKeyword().trim();
        String queryPayload = normalizeQueryPayload(request);
        boolean saved = Boolean.TRUE.equals(request.getSaved());

        Optional<SearchHistoryRecord> existing = StringUtils.hasText(queryPayload)
            ? searchHistoryRecordRepository.findByUserIdAndQueryPayload(userId, queryPayload)
            : searchHistoryRecordRepository.findByUserIdAndKeywordAndSavedFalse(userId, keyword);

        SearchHistoryRecord record = existing.orElseGet(SearchHistoryRecord::new);
        record.setUserId(userId);
        record.setKeyword(keyword);
        record.setResultCount(request.getResultCount() == null ? 0 : request.getResultCount());
        record.setTargetPath(request.getTargetPath());
        record.setQueryPayload(queryPayload);
        record.setSaved(saved);
        record.setLabel(request.getLabel());
        record.setUsageCount(existing.map(item -> item.getUsageCount() + 1).orElse(1));
        record.setTimestamp(LocalDateTime.now());

        SearchHistoryRecord savedRecord = searchHistoryRecordRepository.save(record);
        trimRecentHistory(userId);
        return savedRecord;
    }

    @Transactional
    public void clearHistory(Long userId) {
        searchHistoryRecordRepository.deleteByUserId(userId);
    }

    private void trimRecentHistory(Long userId) {
        List<SearchHistoryRecord> recents = searchHistoryRecordRepository.findByUserIdAndSavedFalseOrderByTimestampDesc(userId);
        if (recents.size() <= MAX_RECENT_HISTORY) {
            return;
        }

        recents.stream()
            .skip(MAX_RECENT_HISTORY)
            .forEach(searchHistoryRecordRepository::delete);
    }

    private String normalizeQueryPayload(SearchHistoryRequest request) {
        if (StringUtils.hasText(request.getQueryPayload())) {
            return request.getQueryPayload();
        }
        if (StringUtils.hasText(request.getTargetPath())) {
            return request.getTargetPath();
        }
        return request.getKeyword().trim();
    }
}
