package com.library.service.statistics;

import com.library.dto.PurchaseSuggestionDTO;
import com.library.dto.PurchaseSuggestionSummaryDTO;
import com.library.model.Book;
import com.library.model.ReservationRecord;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购建议服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSuggestionService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRecordRepository reservationRecordRepository;

    // 采购建议阈值
    private static final int MIN_BORROW_COUNT = 5;
    private static final double HIGH_DEMAND_RATIO = 0.7;

    /**
     * 获取采购建议
     * @return 采购建议汇总
     */
    public PurchaseSuggestionSummaryDTO getPurchaseSuggestions() {
        List<Book> allBooks = bookRepository.findAll();

        // 一次性获取所有书籍的借阅次数
        List<Object[]> borrowCounts = borrowRecordRepository.countBorrowsByBookId();
        Map<Long, Long> bookBorrowCountMap = new HashMap<>();
        for (Object[] arr : borrowCounts) {
            bookBorrowCountMap.put((Long) arr[0], (Long) arr[1]);
        }

        List<PurchaseSuggestionDTO> suggestions = new ArrayList<>();

        int highPriorityCount = 0;
        int mediumPriorityCount = 0;
        int lowPriorityCount = 0;

        for (Book book : allBooks) {
            // 获取借阅次数
            Long borrowCount = bookBorrowCountMap.getOrDefault(book.getId(), 0L);
            if (borrowCount < MIN_BORROW_COUNT) {
                continue;
            }

            // 获取预约数量
            Long reservationCount = reservationRecordRepository.countByBookIdAndStatus(
                book.getId(),
                ReservationRecord.ReservationStatus.WAITING
            );

            // 计算需求比率
            double demandRatio = book.getTotalCopies() > 0 ?
                (double) (book.getTotalCopies() - book.getAvailableCopies()) / book.getTotalCopies() : 0;

            // 判断是否需要采购
            if (demandRatio >= HIGH_DEMAND_RATIO || (reservationCount != null && reservationCount > 0)) {
                PurchaseSuggestionDTO suggestion = createSuggestion(
                    book,
                    borrowCount,
                    reservationCount != null ? reservationCount : 0L,
                    demandRatio
                );

                suggestions.add(suggestion);

                // 统计优先级
                switch (suggestion.getPriority()) {
                    case "HIGH":
                        highPriorityCount++;
                        break;
                    case "MEDIUM":
                        mediumPriorityCount++;
                        break;
                    case "LOW":
                        lowPriorityCount++;
                        break;
                }
            }
        }

        // 按优先级分数排序
        suggestions.sort(Comparator.comparing(PurchaseSuggestionDTO::getScore).reversed());

        // 计算总额外副本数和预估预算
        int totalAdditionalCopies = suggestions.stream()
            .mapToInt(PurchaseSuggestionDTO::getAdditionalCopies)
            .sum();
        double estimatedBudget = totalAdditionalCopies * 50.0; // 假设每本书50元

        return new PurchaseSuggestionSummaryDTO(
            suggestions.size(),
            highPriorityCount,
            mediumPriorityCount,
            lowPriorityCount,
            totalAdditionalCopies,
            estimatedBudget,
            suggestions
        );
    }

    /**
     * 创建采购建议
     */
    private PurchaseSuggestionDTO createSuggestion(Book book, Long borrowCount, Long reservationCount, double demandRatio) {
        int currentCopies = book.getTotalCopies();

        // 计算建议采购数量
        int suggestedCopies = calculateSuggestedCopies(currentCopies, borrowCount, reservationCount, demandRatio);

        // 计算平均等待时间
        double averageWaitTime = calculateAverageWaitTime(book.getId());

        // 生成建议原因
        String reason = generateReason(borrowCount, reservationCount, demandRatio, averageWaitTime);

        // 计算优先级分数
        double score = calculatePriorityScore(borrowCount, reservationCount, demandRatio, averageWaitTime);

        // 确定优先级
        String priority;
        if (score >= 100.0) {
            priority = "HIGH";
        } else if (score >= 60.0) {
            priority = "MEDIUM";
        } else {
            priority = "LOW";
        }

        int additionalCopies = suggestedCopies - currentCopies;

        return new PurchaseSuggestionDTO(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getIsbn(),
            book.getCategory(),
            currentCopies,
            suggestedCopies,
            additionalCopies,
            borrowCount,
            Math.round(averageWaitTime * 100.0) / 100.0,
            reservationCount.intValue(),
            reason,
            priority,
            Math.round(score * 100.0) / 100.0,
            book.getCoverUrl()
        );
    }

    /**
     * 计算建议采购数量
     */
    private int calculateSuggestedCopies(int currentCopies, Long borrowCount, Long reservationCount, double demandRatio) {
        // 基础建议：当前副本数 + 预约数量
        int baseSuggestion = currentCopies + reservationCount.intValue();

        // 根据需求比率调整
        if (demandRatio >= 0.9) {
            baseSuggestion += 2;
        } else if (demandRatio >= 0.8) {
            baseSuggestion += 1;
        }

        // 根据借阅热度调整
        if (borrowCount > 50) {
            baseSuggestion += 2;
        } else if (borrowCount > 20) {
            baseSuggestion += 1;
        }

        return Math.max(baseSuggestion, currentCopies + 1);
    }

    /**
     * 计算平均等待时间
     */
    private double calculateAverageWaitTime(Long bookId) {
        List<ReservationRecord> bookRecords = reservationRecordRepository.findByBookIdWithNotifyDate(bookId);

        if (bookRecords.isEmpty()) {
            return 0.0;
        }

        return bookRecords.stream()
            .mapToLong(r -> ChronoUnit.DAYS.between(
                r.getReservationDate().toLocalDate(),
                r.getNotifyDate().toLocalDate()
            ))
            .average()
            .orElse(0.0);
    }

    /**
     * 生成建议原因
     */
    private String generateReason(Long borrowCount, Long reservationCount, double demandRatio, double averageWaitTime) {
        List<String> reasons = new ArrayList<>();

        if (demandRatio >= 0.9) {
            reasons.add("借阅率超过90%");
        } else if (demandRatio >= 0.8) {
            reasons.add("借阅率超过80%");
        }

        if (reservationCount > 0) {
            reasons.add(String.format("有%d人预约等待", reservationCount));
        }

        if (averageWaitTime > 7) {
            reasons.add(String.format("平均等待%.1f天", averageWaitTime));
        }

        if (borrowCount > 50) {
            reasons.add("借阅次数超过50次");
        }

        return reasons.isEmpty() ? "需求旺盛" : String.join("，", reasons);
    }

    /**
     * 计算优先级分数
     */
    private double calculatePriorityScore(Long borrowCount, Long reservationCount, double demandRatio, double averageWaitTime) {
        double score = 0.0;

        // 借阅次数权重 (0-40分)
        score += Math.min(borrowCount / 2.0, 40.0);

        // 预约数量权重 (0-30分)
        score += Math.min(reservationCount * 10.0, 30.0);

        // 需求比率权重 (0-20分)
        score += demandRatio * 20.0;

        // 等待时间权重 (0-10分)
        score += Math.min(averageWaitTime, 10.0);

        return score;
    }
}
