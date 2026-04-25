package com.library.service.fallback;

import com.library.model.BorrowRecord.BorrowStatus;
import com.library.model.ReservationRecord.ReservationStatus;
import com.library.repository.BookRepository;
import com.library.repository.BookRepository.InventoryStatisticsAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MySQL 统计服务（降级方案）
 * 当 Elasticsearch 不可用时使用 MySQL 进行统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MysqlStatisticsService {

    private final BookRepository bookRepository;

    /**
     * 获取总册数统计
     */
    public Long getTotalCopiesSum() {
        try {
            Long sum = bookRepository.sumTotalCopies();
            return sum != null ? sum : 0L;
        } catch (Exception e) {
            log.error("Failed to get total copies sum from MySQL", e);
            return 0L;
        }
    }

    /**
     * 获取可用册数统计
     */
    public Long getAvailableCopiesSum() {
        try {
            Long sum = bookRepository.sumAvailableCopies();
            return sum != null ? sum : 0L;
        } catch (Exception e) {
            log.error("Failed to get available copies sum from MySQL", e);
            return 0L;
        }
    }

    /**
     * 批量获取统计数据
     */
    public Map<String, Long> getInventoryStatistics() {
        try {
            InventoryStatisticsAggregate aggregate = bookRepository.getInventoryStatisticsAggregate(
                BorrowStatus.OVERDUE.name(),
                ReservationStatus.WAITING.name()
            );

            if (aggregate == null) {
                return Map.of(
                    "totalCopies", 0L,
                    "availableCopies", 0L,
                    "overdueBooks", 0L,
                    "reservedBooks", 0L
                );
            }

            return Map.of(
                "totalCopies", toLong(aggregate.getTotalCopies()),
                "availableCopies", toLong(aggregate.getAvailableCopies()),
                "overdueBooks", toLong(aggregate.getOverdueBooks()),
                "reservedBooks", toLong(aggregate.getReservedBooks())
            );
        } catch (Exception e) {
            log.error("Failed to get inventory statistics from MySQL", e);
            return Map.of(
                "totalCopies", 0L,
                "availableCopies", 0L,
                "overdueBooks", 0L,
                "reservedBooks", 0L
            );
        }
    }

    private long toLong(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
