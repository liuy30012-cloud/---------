package com.library.repository;

import com.library.model.SearchHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRecordRepository extends JpaRepository<SearchHistoryRecord, Long> {

    List<SearchHistoryRecord> findByUserIdOrderBySavedDescTimestampDesc(Long userId);

    List<SearchHistoryRecord> findByUserIdAndSavedFalseOrderByTimestampDesc(Long userId);

    Optional<SearchHistoryRecord> findByUserIdAndQueryPayload(Long userId, String queryPayload);

    Optional<SearchHistoryRecord> findByUserIdAndKeywordAndSavedFalse(Long userId, String keyword);

    void deleteByUserId(Long userId);
}
