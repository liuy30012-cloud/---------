package com.library.repository;

import com.library.model.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
