package com.library.service.statistics;

import com.library.dto.InventoryAlertDTO;
import com.library.dto.InventoryAlertSummaryDTO;
import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存预警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAlertService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRecordRepository reservationRecordRepository;

    // 预警阈值
    private static final int LOW_STOCK_THRESHOLD = 2;
    private static final double HIGH_DEMAND_THRESHOLD = 0.8;

    /**
     * 获取库存预警信息
     * @return 库存预警汇总
     */
    public InventoryAlertSummaryDTO getInventoryAlerts() {
        List<Book> allBooks = bookRepository.findAll();
        List<InventoryAlertDTO> alerts = new ArrayList<>();

        int criticalCount = 0;
        int warningCount = 0;
        int outOfStockCount = 0;
        int lowStockCount = 0;
        int highDemandCount = 0;

        for (Book book : allBooks) {
            String alertType = null;
            String severity = null;
            String message = null;

            // 检查缺货
            if (book.getAvailableCopies() == 0 && book.getTotalCopies() > 0) {
                alertType = "OUT_OF_STOCK";
                severity = "CRITICAL";
                message = "该书籍已全部借出，无可借副本";
                criticalCount++;
                outOfStockCount++;
            }
            // 检查低库存
            else if (book.getAvailableCopies() > 0 && book.getAvailableCopies() <= LOW_STOCK_THRESHOLD) {
                alertType = "LOW_STOCK";
                severity = "WARNING";
                message = String.format("可借副本仅剩 %d 本，建议补充库存", book.getAvailableCopies());
                warningCount++;
                lowStockCount++;
            }
            // 检查高需求（借阅率超过80%）
            else if (book.getTotalCopies() > 0) {
                double borrowRate = (double) (book.getTotalCopies() - book.getAvailableCopies()) / book.getTotalCopies();
                if (borrowRate >= HIGH_DEMAND_THRESHOLD) {
                    alertType = "HIGH_DEMAND";
                    severity = "WARNING";
                    message = String.format("借阅率达 %.0f%%，需求旺盛，建议增加副本", borrowRate * 100);
                    warningCount++;
                    highDemandCount++;
                }
            }

            if (alertType != null) {
                // 计算已借出副本数
                int borrowedCopies = book.getTotalCopies() - book.getAvailableCopies();

                // 获取借阅次数
                Long borrowCount = borrowRecordRepository.countByBookId(book.getId());

                alerts.add(new InventoryAlertDTO(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getIsbn(),
                    book.getCategory(),
                    book.getTotalCopies(),
                    book.getAvailableCopies(),
                    borrowedCopies,
                    borrowCount != null ? borrowCount : 0L,
                    alertType,
                    severity,
                    message,
                    book.getCoverUrl()
                ));
            }
        }

        // 按严重程度和借阅次数排序
        alerts.sort((a, b) -> {
            int severityCompare = getSeverityOrder(a.getAlertLevel()) - getSeverityOrder(b.getAlertLevel());
            if (severityCompare != 0) return severityCompare;
            return Long.compare(b.getBorrowCount(), a.getBorrowCount());
        });

        return new InventoryAlertSummaryDTO(
            alerts.size(),
            criticalCount,
            warningCount,
            outOfStockCount,
            lowStockCount,
            highDemandCount,
            alerts
        );
    }

    private int getSeverityOrder(String severity) {
        return "CRITICAL".equals(severity) ? 0 : 1;
    }
}
