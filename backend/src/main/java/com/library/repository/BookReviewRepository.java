package com.library.repository;

import com.library.model.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    /**
     * 根据书籍ID查询所有评价（分页）
     */
    Page<BookReview> findByBookId(Long bookId, Pageable pageable);

    /**
     * 根据用户ID查询所有评价（分页）
     */
    Page<BookReview> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据书籍ID和用户ID查询评价
     */
    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    /**
     * 检查用户是否已评价某本书
     */
    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    /**
     * 计算书籍的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM BookReview r WHERE r.bookId = :bookId")
    Double calculateAverageRating(@Param("bookId") Long bookId);

    /**
     * 统计书籍的评价数量
     */
    long countByBookId(Long bookId);

    /**
     * 根据评分查询书籍评价
     */
    Page<BookReview> findByBookIdAndRating(Long bookId, Integer rating, Pageable pageable);

    /**
     * 查询书籍的最新评价
     */
    List<BookReview> findTop10ByBookIdOrderByCreatedAtDesc(Long bookId);

    /**
     * 根据用户ID查询所有评价(按创建时间倒序)
     */
    List<BookReview> findByUserIdOrderByCreatedAtDesc(Long userId);
}
