package com.library.repository;

import com.library.model.ReadingStatus;
import com.library.model.ReadingStatusRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingStatusRecordRepository extends JpaRepository<ReadingStatusRecord, Long> {

    Optional<ReadingStatusRecord> findByUserIdAndBookId(Long userId, Long bookId);

    List<ReadingStatusRecord> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ReadingStatusRecord> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ReadingStatus status);

    long countByUserIdAndStatus(Long userId, ReadingStatus status);

    long countByUserId(Long userId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    Page<ReadingStatusRecord> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Page<ReadingStatusRecord> findByUserIdAndStatusOrderByUpdatedAtDesc(
        Long userId,
        ReadingStatus status,
        Pageable pageable
    );
}
