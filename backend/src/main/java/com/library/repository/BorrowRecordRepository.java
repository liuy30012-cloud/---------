package com.library.repository;

import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<BorrowRecord> findByUserIdAndStatus(Long userId, BorrowStatus status);

    List<BorrowRecord> findByUserIdAndStatusOrderByDueDateAsc(Long userId, BorrowStatus status);

    List<BorrowRecord> findByUserIdAndStatusIn(Long userId, List<BorrowStatus> statuses);

    @Query("SELECT COUNT(b) FROM BorrowRecord b WHERE b.userId = :userId AND b.status IN (:statuses)")
    long countCurrentBorrowsByUserId(@Param("userId") Long userId, @Param("statuses") List<BorrowStatus> statuses);

    List<BorrowRecord> findByStatusAndDueDateBefore(BorrowStatus status, LocalDateTime date);

    List<BorrowRecord> findByBookIdAndStatusIn(Long bookId, List<BorrowStatus> statuses);

    List<BorrowRecord> findByStatusOrderByCreatedAtAsc(BorrowStatus status);

    List<BorrowRecord> findTop5ByBookIdOrderByCreatedAtDesc(Long bookId);

    boolean existsByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, List<BorrowStatus> statuses);

    @Query("SELECT b.bookId, COUNT(b) FROM BorrowRecord b GROUP BY b.bookId")
    List<Object[]> countBorrowsByBookId();

    long countByBookId(Long bookId);

    long countByBookIdAndStatusIn(Long bookId, List<BorrowStatus> statuses);

    List<BorrowRecord> findByBookId(Long bookId);

    List<BorrowRecord> findByUserId(Long userId);

    @Query("SELECT b FROM BorrowRecord b WHERE b.createdAt >= :startDate")
    List<BorrowRecord> findByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT DATE(b.borrowDate) as date, COUNT(b) FROM BorrowRecord b " +
           "WHERE b.borrowDate >= :startDate GROUP BY DATE(b.borrowDate)")
    List<Object[]> countBorrowsByDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT b FROM BorrowRecord b WHERE b.status = :status AND b.dueDate BETWEEN :startDate AND :endDate")
    List<BorrowRecord> findByStatusAndDueDateBetween(
        @Param("status") BorrowStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT DATE(b.returnDate) as date, COUNT(b) FROM BorrowRecord b " +
           "WHERE b.returnDate IS NOT NULL AND b.returnDate >= :startDate GROUP BY DATE(b.returnDate)")
    List<Object[]> countReturnsByDate(@Param("startDate") LocalDateTime startDate);

    long countByStatus(BorrowStatus status);

    Page<BorrowRecord> findByUserIdOrderByBorrowTimeDesc(Long userId, Pageable pageable);

    Page<BorrowRecord> findByUserIdAndStatusInOrderByBorrowTimeDesc(
        Long userId,
        List<BorrowStatus> statuses,
        Pageable pageable
    );

    Page<BorrowRecord> findByStatusOrderByApplyTimeAsc(BorrowStatus status, Pageable pageable);
}
