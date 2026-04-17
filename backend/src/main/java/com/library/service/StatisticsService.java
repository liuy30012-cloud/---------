package com.library.service;

import com.library.dto.BorrowTrendDTO;
import com.library.dto.CategoryStatisticsDTO;
import com.library.dto.DashboardDataDTO;
import com.library.dto.InventoryAlertSummaryDTO;
import com.library.dto.InventoryStatisticsDTO;
import com.library.dto.PopularBookDTO;
import com.library.dto.PurchaseSuggestionSummaryDTO;
import com.library.dto.UserProfileDTO;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.model.ReservationRecord.ReservationStatus;
import com.library.service.elasticsearch.ElasticsearchStatisticsService;
import com.library.service.fallback.MysqlStatisticsService;
import com.library.service.statistics.BorrowTrendService;
import com.library.service.statistics.CategoryStatisticsService;
import com.library.service.statistics.InventoryAlertService;
import com.library.service.statistics.PopularBooksService;
import com.library.service.statistics.PurchaseSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PopularBooksService popularBooksService;
    private final BorrowTrendService borrowTrendService;
    private final CategoryStatisticsService categoryStatisticsService;
    private final InventoryAlertService inventoryAlertService;
    private final PurchaseSuggestionService purchaseSuggestionService;
    private final UserService userService;
    private final com.library.repository.BookRepository bookRepository;
    private final com.library.repository.BorrowRecordRepository borrowRecordRepository;
    private final com.library.repository.ReservationRecordRepository reservationRecordRepository;
    private final ElasticsearchStatisticsService elasticsearchStatisticsService;
    private final MysqlStatisticsService mysqlStatisticsService;

    @Value("${library.search.elasticsearch.enabled:true}")
    private boolean elasticsearchEnabled;

    public List<PopularBookDTO> getPopularBooks(int limit) {
        return popularBooksService.getPopularBooks(limit);
    }

    public List<BorrowTrendDTO> getBorrowTrends(int days) {
        return borrowTrendService.getBorrowTrends(days);
    }

    public List<CategoryStatisticsDTO> getCategoryStatistics() {
        return categoryStatisticsService.getCategoryStatistics();
    }

    public UserProfileDTO getUserProfile(Long userId) {
        var user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        List<BorrowRecord> userRecords = borrowRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userRecords.isEmpty()) {
            return new UserProfileDTO(
                userId,
                user.getStudentId(),
                user.getUsername(),
                0L,
                0L,
                "未知",
                0.0
            );
        }

        long totalBorrows = userRecords.size();
        long currentBorrows = userRecords.stream()
            .filter(record -> record.getStatus() == BorrowStatus.BORROWED || record.getStatus() == BorrowStatus.OVERDUE)
            .count();

        Map<Long, Book> bookMap = new HashMap<>();
        List<Long> bookIds = userRecords.stream()
            .map(BorrowRecord::getBookId)
            .distinct()
            .toList();
        if (!bookIds.isEmpty()) {
            bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
        }

        String favoriteCategory = userRecords.stream()
            .map(BorrowRecord::getBookId)
            .map(bookMap::get)
            .filter(Objects::nonNull)
            .map(Book::getCategory)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("未知");

        double averageBorrowDays = userRecords.stream()
            .filter(record -> record.getReturnDate() != null)
            .mapToLong(record -> ChronoUnit.DAYS.between(record.getBorrowDate(), record.getReturnDate()))
            .average()
            .orElse(0.0);

        return new UserProfileDTO(
            userId,
            user.getStudentId(),
            user.getUsername(),
            totalBorrows,
            currentBorrows,
            favoriteCategory,
            Math.round(averageBorrowDays * 100.0) / 100.0
        );
    }

    public InventoryStatisticsDTO getInventoryStatistics() {
        long totalBooks;
        long availableBooks;

        // 优先使用 Elasticsearch，失败时降级到 MySQL
        if (elasticsearchEnabled) {
            try {
                if (elasticsearchStatisticsService.isAvailable()) {
                    Map<String, Long> stats = elasticsearchStatisticsService.getInventoryStatistics();
                    totalBooks = stats.get("totalCopies");
                    availableBooks = stats.get("availableCopies");
                    log.debug("Using Elasticsearch for inventory statistics");
                } else {
                    log.warn("Elasticsearch is not available, falling back to MySQL");
                    Map<String, Long> stats = mysqlStatisticsService.getInventoryStatistics();
                    totalBooks = stats.get("totalCopies");
                    availableBooks = stats.get("availableCopies");
                }
            } catch (Exception e) {
                log.error("Elasticsearch query failed, falling back to MySQL", e);
                Map<String, Long> stats = mysqlStatisticsService.getInventoryStatistics();
                totalBooks = stats.get("totalCopies");
                availableBooks = stats.get("availableCopies");
            }
        } else {
            log.debug("Elasticsearch is disabled, using MySQL");
            Map<String, Long> stats = mysqlStatisticsService.getInventoryStatistics();
            totalBooks = stats.get("totalCopies");
            availableBooks = stats.get("availableCopies");
        }

        long borrowedBooks = totalBooks - availableBooks;
        long overdueBooks = borrowRecordRepository.countByStatus(BorrowStatus.OVERDUE);
        long reservedBooks = reservationRecordRepository.countByStatus(ReservationStatus.WAITING);
        double utilizationRate = totalBooks > 0 ? (borrowedBooks * 100.0 / totalBooks) : 0.0;

        return new InventoryStatisticsDTO(
            totalBooks,
            availableBooks,
            borrowedBooks,
            overdueBooks,
            reservedBooks,
            Math.round(utilizationRate * 100.0) / 100.0
        );
    }

    public InventoryAlertSummaryDTO getInventoryAlerts() {
        return inventoryAlertService.getInventoryAlerts();
    }

    public PurchaseSuggestionSummaryDTO getPurchaseSuggestions() {
        return purchaseSuggestionService.getPurchaseSuggestions();
    }

    public DashboardDataDTO getDashboardData() {
        return new DashboardDataDTO(
            getInventoryStatistics(),
            getPopularBooks(10),
            getBorrowTrends(30),
            getCategoryStatistics(),
            userService.getUserCount(),
            userService.getActiveUserCount()
        );
    }
}
