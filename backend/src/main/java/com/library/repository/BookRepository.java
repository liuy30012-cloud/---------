package com.library.repository;

import com.library.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    interface InventoryStatisticsAggregate {
        Number getTotalCopies();
        Number getAvailableCopies();
        Number getOverdueBooks();
        Number getReservedBooks();
    }

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findByTitleContaining(String title);

    @Query("SELECT b FROM Book b " +
           "WHERE REPLACE(REPLACE(UPPER(COALESCE(b.isbn, '')), '-', ''), ' ', '') = :normalizedIsbn")
    Optional<Book> findFirstByNormalizedIsbn(@Param("normalizedIsbn") String normalizedIsbn);

    @Query("SELECT b FROM Book b " +
           "WHERE LOWER(TRIM(b.title)) = LOWER(TRIM(:title)) " +
           "AND LOWER(TRIM(COALESCE(b.author, ''))) = LOWER(TRIM(COALESCE(:author, '')))")
    Optional<Book> findFirstByNormalizedTitleAndAuthor(@Param("title") String title, @Param("author") String author);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies - 1, b.borrowedCount = b.borrowedCount + 1 " +
           "WHERE b.id = :id AND b.availableCopies > 0")
    int decreaseAvailableCopies(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1, b.borrowedCount = b.borrowedCount - 1 " +
           "WHERE b.id = :id AND b.availableCopies < b.totalCopies AND b.borrowedCount > 0")
    int increaseAvailableCopies(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " b.title LIKE CONCAT('%', :keyword, '%') OR " +
           " b.author LIKE CONCAT('%', :keyword, '%') OR " +
           " b.isbn LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:author IS NULL OR :author = '' OR b.author LIKE CONCAT('%', :author, '%')) AND " +
           "(:year IS NULL OR :year = '' OR b.year = :year) AND " +
           "(:category IS NULL OR :category = '' OR b.category = :category) AND " +
           "(:language IS NULL OR :language = '' OR b.languageCode = :language) AND " +
           "(:status IS NULL OR :status = '' OR " +
           " (:status = 'AVAILABLE' AND b.availableCopies > 0) OR " +
           " (:status = 'CHECKED_OUT' AND b.availableCopies <= 0))")
    Page<Book> searchCatalog(@Param("keyword") String keyword,
                             @Param("author") String author,
                             @Param("year") String year,
                             @Param("category") String category,
                             @Param("language") String language,
                             @Param("status") String status,
                             Pageable pageable);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL AND b.category <> '' ORDER BY b.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT b.languageCode FROM Book b WHERE b.languageCode IS NOT NULL AND b.languageCode <> '' ORDER BY b.languageCode")
    List<String> findDistinctLanguages();

    @Query("SELECT b.isbn FROM Book b WHERE b.isbn IS NOT NULL AND b.isbn <> ''")
    List<String> findAllIsbns();

    List<Book> findTop6ByCategoryAndIdNotOrderByBorrowedCountDesc(String category, Long id);

    List<Book> findTop6ByAuthorAndIdNotOrderByBorrowedCountDesc(String author, Long id);

    @Query("SELECT SUM(b.totalCopies) FROM Book b")
    Long sumTotalCopies();

    @Query("SELECT SUM(b.availableCopies) FROM Book b")
    Long sumAvailableCopies();

    @Query(value = """
        SELECT
            COALESCE(SUM(b.total_copies), 0) AS totalCopies,
            COALESCE(SUM(b.available_copies), 0) AS availableCopies,
            (SELECT COUNT(*) FROM borrow_records br WHERE br.status = :overdueStatus) AS overdueBooks,
            (SELECT COUNT(*) FROM reservation_records rr WHERE rr.status = :waitingStatus) AS reservedBooks
        FROM books b
        """, nativeQuery = true)
    InventoryStatisticsAggregate getInventoryStatisticsAggregate(
        @Param("overdueStatus") String overdueStatus,
        @Param("waitingStatus") String waitingStatus
    );
}
