package com.library.repository;

import com.library.model.BookFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookFavoriteRepository extends JpaRepository<BookFavorite, Long> {

    Optional<BookFavorite> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    List<BookFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT bf.bookId FROM BookFavorite bf WHERE bf.userId = :userId AND bf.bookId IN :bookIds")
    Set<Long> findFavoritedBookIds(@Param("userId") Long userId, @Param("bookIds") List<Long> bookIds);
}
