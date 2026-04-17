package com.library.service.fallback;

import com.library.dto.InventoryStatisticsDTO;
import com.library.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"library.search.elasticsearch.enabled=false"})
class FallbackTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void testMysqlFallback() {
        InventoryStatisticsDTO stats = statisticsService.getInventoryStatistics();

        assertNotNull(stats);
        assertTrue(stats.getTotalBooks() >= 0);
        assertTrue(stats.getAvailableBooks() >= 0);
    }
}
