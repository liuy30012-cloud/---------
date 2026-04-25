package com.library.service;

import com.library.dto.InventoryStatisticsDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StatisticsServiceIntegrationTest {

    private static final long MAX_DURATION_NANOS = Duration.ofMillis(500).toNanos();

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
        long firstStart = System.nanoTime();
        InventoryStatisticsDTO firstStats = statisticsService.getInventoryStatistics();
        long firstDuration = System.nanoTime() - firstStart;

        long secondStart = System.nanoTime();
        InventoryStatisticsDTO secondStats = statisticsService.getInventoryStatistics();
        long secondDuration = System.nanoTime() - secondStart;

        assertNotNull(firstStats);
        assertEquals(firstStats, secondStats);
        assertDurationUnderLimit("first", firstDuration);
        assertDurationUnderLimit("second", secondDuration);
    }

    private void assertDurationUnderLimit(String label, long durationNanos) {
        assertTrue(
            durationNanos < MAX_DURATION_NANOS,
            "inventory statistics " + label + " call should stay under 500ms, actual: " + formatMillis(durationNanos) + "ms"
        );
    }

    private double formatMillis(long durationNanos) {
        return durationNanos / 1_000_000.0;
    }
}
