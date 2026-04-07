package com.library.service.statistics;

import com.library.dto.CategoryStatisticsDTO;
import com.library.model.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryStatisticsService {

    private final BookRepository bookRepository;

    /**
     * 获取分类统计信息
     * @return 分类统计列表
     */
    public List<CategoryStatisticsDTO> getCategoryStatistics() {
        List<Book> allBooks = bookRepository.findAll();

        // 按分类分组
        Map<String, List<Book>> booksByCategory = allBooks.stream()
            .filter(book -> book.getCategory() != null && !book.getCategory().isEmpty())
            .collect(Collectors.groupingBy(Book::getCategory));

        return booksByCategory.entrySet().stream()
            .map(entry -> {
                String category = entry.getKey();
                List<Book> books = entry.getValue();

                // 统计总副本数
                long totalBooks = books.stream()
                    .mapToLong(Book::getTotalCopies)
                    .sum();

                // 统计已借出的副本数
                long borrowedBooks = books.stream()
                    .mapToLong(b -> b.getTotalCopies() - b.getAvailableCopies())
                    .sum();

                // 统计可借副本数
                long availableBooks = books.stream()
                    .mapToLong(Book::getAvailableCopies)
                    .sum();

                // 计算借阅率
                double borrowRate = totalBooks > 0 ? (borrowedBooks * 100.0 / totalBooks) : 0.0;

                return new CategoryStatisticsDTO(
                    category,
                    totalBooks,
                    borrowedBooks,
                    availableBooks,
                    Math.round(borrowRate * 100.0) / 100.0
                );
            })
            .sorted(Comparator.comparing(CategoryStatisticsDTO::getTotalBooks).reversed())
            .collect(Collectors.toList());
    }
}
