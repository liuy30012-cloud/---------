package com.library.service;

import com.library.dto.BookRatingStatistics;
import com.library.dto.BookReviewRequest;
import com.library.dto.BookReviewResponse;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.model.BookReview;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.BookReviewRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * 创建书籍评价
     */
    @Transactional
    public BookReviewResponse createReview(Long userId, BookReviewRequest request) {
        // 验证书籍是否存在
        if (!bookRepository.existsById(request.getBookId())) {
            throw new ResourceNotFoundException("Book not found with id: " + request.getBookId());
        }

        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 检查用户是否已评价过该书籍
        if (bookReviewRepository.existsByBookIdAndUserId(request.getBookId(), userId)) {
            throw new IllegalStateException("You have already reviewed this book");
        }

        // 创建评价
        BookReview review = new BookReview();
        review.setBookId(request.getBookId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        BookReview savedReview = bookReviewRepository.save(review);
        return convertToResponse(savedReview, user.getUsername());
    }

    /**
     * 更新书籍评价
     */
    @Transactional
    public BookReviewResponse updateReview(Long reviewId, Long userId, BookReviewRequest request) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // 验证评价所有者
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own reviews");
        }

        // 更新评价
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        BookReview updatedReview = bookReviewRepository.save(review);
        User user = userRepository.findById(userId).orElseThrow();
        return convertToResponse(updatedReview, user.getUsername());
    }

    /**
     * 删除书籍评价
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // 验证评价所有者
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        bookReviewRepository.delete(review);
    }

    /**
     * 获取书籍的所有评价（分页）
     */
    public Page<BookReviewResponse> getBookReviews(Long bookId, int page, int size, String sortBy) {
        // 验证书籍是否存在
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BookReview> reviews = bookReviewRepository.findByBookId(bookId, pageable);
        return reviews.map(review -> {
            User user = userRepository.findById(review.getUserId()).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            return convertToResponse(review, username);
        });
    }

    /**
     * 获取用户的所有评价（分页）
     */
    public Page<BookReviewResponse> getUserReviews(Long userId, int page, int size) {
        // 验证用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BookReview> reviews = bookReviewRepository.findByUserId(userId, pageable);

        User user = userRepository.findById(userId).orElseThrow();
        return reviews.map(review -> convertToResponse(review, user.getUsername()));
    }

    /**
     * 获取单个评价详情
     */
    public BookReviewResponse getReviewById(Long reviewId) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        User user = userRepository.findById(review.getUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "Unknown";
        return convertToResponse(review, username);
    }

    /**
     * 获取书籍评分统计
     */
    public BookRatingStatistics getBookRatingStatistics(Long bookId) {
        // 验证书籍是否存在
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }

        Double averageRating = bookReviewRepository.calculateAverageRating(bookId);
        long totalReviews = bookReviewRepository.countByBookId(bookId);

        // 统计各星级数量
        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0L);
        }

        List<BookReview> allReviews = bookReviewRepository.findByBookId(bookId, Pageable.unpaged()).getContent();
        for (BookReview review : allReviews) {
            ratingCounts.merge(review.getRating(), 1L, Long::sum);
        }

        return new BookRatingStatistics(
                bookId,
                averageRating != null ? averageRating : 0.0,
                totalReviews,
                ratingCounts.get(5),
                ratingCounts.get(4),
                ratingCounts.get(3),
                ratingCounts.get(2),
                ratingCounts.get(1)
        );
    }

    /**
     * 获取书籍的最新评价
     */
    public List<BookReviewResponse> getLatestReviews(Long bookId, int limit) {
        // 验证书籍是否存在
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }

        List<BookReview> reviews = bookReviewRepository.findTop10ByBookIdOrderByCreatedAtDesc(bookId);
        return reviews.stream()
                .limit(limit)
                .map(review -> {
                    User user = userRepository.findById(review.getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "Unknown";
                    return convertToResponse(review, username);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private BookReviewResponse convertToResponse(BookReview review, String username) {
        return new BookReviewResponse(
                review.getId(),
                review.getBookId(),
                review.getUserId(),
                username,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
