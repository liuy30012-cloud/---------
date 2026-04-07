package com.library.repository;

import com.library.model.ReservationRecord;
import com.library.model.ReservationRecord.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRecordRepository extends JpaRepository<ReservationRecord, Long> {

    List<ReservationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ReservationRecord> findByBookIdAndStatusOrderByCreatedAtAsc(Long bookId, ReservationStatus status);

    List<ReservationRecord> findByBookIdAndStatusInOrderByCreatedAtAsc(Long bookId, List<ReservationStatus> statuses);

    List<ReservationRecord> findByStatusAndExpireDateBefore(ReservationStatus status, LocalDateTime date);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, ReservationStatus status);

    @Query("SELECT COUNT(r) FROM ReservationRecord r WHERE r.bookId = :bookId AND r.status = com.library.model.ReservationRecord$ReservationStatus.WAITING")
    long countWaitingReservationsByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(r) FROM ReservationRecord r WHERE r.bookId = :bookId AND r.userId != :userId " +
           "AND r.status IN (com.library.model.ReservationRecord$ReservationStatus.WAITING, com.library.model.ReservationRecord$ReservationStatus.AVAILABLE)")
    long countActiveOtherReservations(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ReservationRecord r SET r.queuePosition = :position WHERE r.id = :id")
    void updateQueuePosition(@Param("id") Long id, @Param("position") Integer position);

    long countByStatus(ReservationStatus status);

    @Query("SELECT r FROM ReservationRecord r WHERE r.bookId = :bookId AND r.notifyDate IS NOT NULL")
    List<ReservationRecord> findByBookIdWithNotifyDate(@Param("bookId") Long bookId);

    @Query("SELECT r.bookId, COUNT(r) FROM ReservationRecord r WHERE r.status = :status GROUP BY r.bookId")
    List<Object[]> countReservationsByBookIdAndStatus(@Param("status") ReservationStatus status);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    // 查询即将过期的预约(用于定时提醒)
    List<ReservationRecord> findByStatusAndExpiryReminderSentAndExpireDateBetween(
        ReservationStatus status,
        Boolean expiryReminderSent,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    // 查询用户历史预约(用于计算灵活窗口)
    List<ReservationRecord> findTop10ByUserIdAndStatusInOrderByCreatedAtDesc(
        Long userId,
        List<ReservationStatus> statuses
    );
}
