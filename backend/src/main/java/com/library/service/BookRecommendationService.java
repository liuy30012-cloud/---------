package com.library.service;

import com.library.model.Book;
import com.library.model.BookTag;
import com.library.model.BorrowRecord;
import com.library.repository.BookRepository;
import com.library.repository.BookTagRepository;
import com.library.repository.BorrowRecordRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

    private final BookRepository bookRepository;
    private final BookTagRepository bookTagRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public BookRecommendationService(
        BookRepository bookRepository,
        BookTagRepository bookTagRepository,
        BorrowRecordRepository borrowRecordRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookTagRepository = bookTagRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    /**
     * 基于内容的推荐 - 相似图书
     */
    public List<Book> getContentBasedRecommendations(Long bookId, int limit) {
        Optional<Book> targetBook = bookRepository.findById(bookId);
        if (targetBook.isEmpty()) {
            return Collections.emptyList();
        }

        Book book = targetBook.get();
        List<Book> allBooks = bookRepository.findAll();

        // 计算相似度分数
        List<BookSimilarity> similarities = allBooks.stream()
            .filter(b -> !b.getId().equals(bookId))
            .map(b -> new BookSimilarity(b, calculateSimilarity(book, b)))
            .sorted(Comparator.comparingDouble(BookSimilarity::score).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return similarities.stream()
            .map(BookSimilarity::book)
            .collect(Collectors.toList());
    }

    /**
     * 协同过滤推荐 - "读过这本书的人还读了"
     */
    public List<Book> getCollaborativeRecommendations(Long bookId, int limit) {
        // 1. 找到借阅过这本书的用户
        List<BorrowRecord> borrowRecords = borrowRecordRepository.findByBookId(bookId);
        Set<Long> userIds = borrowRecords.stream()
            .map(BorrowRecord::getUserId)
            .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 找到这些用户借阅的其他书籍
        Map<Long, Integer> bookFrequency = new HashMap<>();
        for (Long userId : userIds) {
            List<BorrowRecord> userBorrows = borrowRecordRepository.findByUserId(userId);
            for (BorrowRecord record : userBorrows) {
                if (!record.getBookId().equals(bookId)) {
                    bookFrequency.merge(record.getBookId(), 1, Integer::sum);
                }
            }
        }

        // 3. 按频率排序并返回
        return bookFrequency.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> bookRepository.findById(entry.getKey()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * 基于标签的推荐
     */
    public List<Book> getTagBasedRecommendations(Long bookId, int limit) {
        List<BookTag> tags = bookTagRepository.findByBookId(bookId);
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }

        // 找到有相同标签的书籍
        Map<Long, Integer> bookTagMatches = new HashMap<>();
        for (BookTag tag : tags) {
            List<Long> booksWithTag = bookTagRepository.findBookIdsByTagName(tag.getTagName());
            for (Long relatedBookId : booksWithTag) {
                if (!relatedBookId.equals(bookId)) {
                    bookTagMatches.merge(relatedBookId, 1, Integer::sum);
                }
            }
        }

        return bookTagMatches.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> bookRepository.findById(entry.getKey()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * 混合推荐 - 结合多种推荐策略
     */
    public List<Book> getHybridRecommendations(Long bookId, int limit) {
        List<Book> contentBased = getContentBasedRecommendations(bookId, limit);
        List<Book> collaborative = getCollaborativeRecommendations(bookId, limit);
        List<Book> tagBased = getTagBasedRecommendations(bookId, limit);

        // 合并并去重
        Map<Long, RecommendationScore> scoreMap = new HashMap<>();

        addToScoreMap(scoreMap, contentBased, 1.0);
        addToScoreMap(scoreMap, collaborative, 1.5); // 协同过滤权重更高
        addToScoreMap(scoreMap, tagBased, 0.8);

        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, RecommendationScore>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> bookRepository.findById(entry.getKey()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * 计算两本书的相似度
     */
    private double calculateSimilarity(Book book1, Book book2) {
        double score = 0.0;

        // 相同作者 +0.4
        if (book1.getAuthor() != null && book1.getAuthor().equals(book2.getAuthor())) {
            score += 0.4;
        }

        // 相同分类 +0.3
        if (book1.getCategory() != null && book1.getCategory().equals(book2.getCategory())) {
            score += 0.3;
        }

        // 相同语言 +0.1
        if (book1.getLanguageCode() != null && book1.getLanguageCode().equals(book2.getLanguageCode())) {
            score += 0.1;
        }

        // 年份接近 +0.2 (5年内)
        if (book1.getYear() != null && book2.getYear() != null) {
            int yearDiff = Math.abs(Integer.parseInt(book1.getYear()) - Integer.parseInt(book2.getYear()));
            if (yearDiff <= 5) {
                score += 0.2 * (1.0 - yearDiff / 5.0);
            }
        }

        return score;
    }

    private void addToScoreMap(Map<Long, RecommendationScore> scoreMap, List<Book> books, double weight) {
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            double positionScore = 1.0 - (i * 0.1); // 位置越靠前分数越高
            scoreMap.merge(
                book.getId(),
                new RecommendationScore(positionScore * weight),
                RecommendationScore::add
            );
        }
    }

    private record BookSimilarity(Book book, double score) {}

    private static class RecommendationScore implements Comparable<RecommendationScore> {
        private double score;

        public RecommendationScore(double score) {
            this.score = score;
        }

        public RecommendationScore add(RecommendationScore other) {
            this.score += other.score;
            return this;
        }

        @Override
        public int compareTo(RecommendationScore other) {
            return Double.compare(this.score, other.score);
        }
    }
}
