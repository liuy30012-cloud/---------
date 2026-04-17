package com.library.service;

import com.library.dto.InventoryStatisticsDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StatisticsServiceIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void testGetInventoryStatistics() {
        InventoryStatisticsDTO stats = statisticsService.getInventoryStatistics();

        assertNotNull(stats);
        assertTrue(stats.getTotalBooks() >= 0);
        assertTrue(stats.getAvailableBooks() >= 0);
        assertTrue(stats.getBorrowedBooks() >= 0);
        assertTrue(stats.getUtilizationRate() >= 0 && stats.getUtilizationRate() <= 100);
    }

    @Test
    void testGetInventoryStatisticsPerformance() {
        long startTime = System.currentTimeMillis();

        statisticsService.getInventoryStatistics();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 500, "统计查询应该在 500ms 内完成，实际耗时: " + duration + "ms");
    }
}
