package com.library.service.fallback;

import com.library.repository.BookRepository;
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
            Long totalCopies = bookRepository.sumTotalCopies();
            Long availableCopies = bookRepository.sumAvailableCopies();

            return Map.of(
                "totalCopies", totalCopies != null ? totalCopies : 0L,
                "availableCopies", availableCopies != null ? availableCopies : 0L
            );
        } catch (Exception e) {
            log.error("Failed to get inventory statistics from MySQL", e);
            return Map.of("totalCopies", 0L, "availableCopies", 0L);
        }
    }
}
