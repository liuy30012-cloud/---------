package com.library.service.statistics;

import com.library.dto.PopularBookDTO;
import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 热门书籍统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularBooksService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;

    /**
     * 获取热门书籍列表
     * @param limit 返回数量限制
     * @return 热门书籍列表
     */
    public List<PopularBookDTO> getPopularBooks(int limit) {
        // 使用数据库聚合查询，一次性获取所有书籍的借阅次数
        List<Object[]> borrowCounts = borrowRecordRepository.countBorrowsByBookId();

        // 转换为Map便于查找
        Map<Long, Long> bookBorrowCountMap = borrowCounts.stream()
            .collect(Collectors.toMap(
                arr -> (Long) arr[0],
                arr -> (Long) arr[1]
            ));

        // 获取Top N的书籍ID
        List<Long> topBookIds = bookBorrowCountMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (topBookIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Book> books = bookRepository.findAllById(topBookIds);
        Map<Long, Book> bookMap = books.stream()
            .collect(Collectors.toMap(Book::getId, book -> book));

        // 按借阅次数排序构建结果，过滤掉已删除的书籍
        return topBookIds.stream()
            .filter(bookMap::containsKey)
            .map(bookId -> {
                Book book = bookMap.get(bookId);
                Long borrowCount = bookBorrowCountMap.get(bookId);
                return new PopularBookDTO(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getIsbn(),
                    borrowCount,
                    book.getCoverUrl()
                );
            })
            .collect(Collectors.toList());
    }
}
