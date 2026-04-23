package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import com.library.repository.BookReviewRepository;
import com.library.repository.BookFavoriteRepository;
import com.library.repository.ReadingStatusRecordRepository;
import com.library.dto.BatchDeleteResponse;
import com.library.dto.BatchOperationFailure;
import com.library.dto.ImportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import static com.library.util.BookImportSupport.normalizeIsbn;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final ReservationRecordRepository reservationRecordRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookFavoriteRepository bookFavoriteRepository;
    private final ReadingStatusRecordRepository readingStatusRecordRepository;

    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElse(null);
    }

    public java.util.Optional<Book> findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    public boolean isIsbnUsedByOther(String isbn, Long excludeId) {
        return bookRepository.findByIsbn(isbn)
            .filter(book -> !book.getId().equals(excludeId))
            .isPresent();
    }

    public List<Book> findTopBooksByCategoryExcluding(String category, Long excludeId, int limit) {
        return bookRepository.findTop6ByCategoryAndIdNotOrderByBorrowedCountDesc(category, excludeId);
    }

    public List<Book> findTopBooksByAuthorExcluding(String author, Long excludeId, int limit) {
        return bookRepository.findTop6ByAuthorAndIdNotOrderByBorrowedCountDesc(author, excludeId);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public void decreaseAvailableCopies(Long bookId) {
        int updated = bookRepository.decreaseAvailableCopies(bookId);

        if (updated == 0) {
            if (!bookRepository.existsById(bookId)) {
                throw new IllegalArgumentException("图书不存在。");
            }
            throw new IllegalArgumentException("当前没有可借副本。");
        }

        log.info("图书 {} 可借数量减少", bookId);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public void increaseAvailableCopies(Long bookId) {
        int updated = bookRepository.increaseAvailableCopies(bookId);

        if (updated == 0) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) {
                throw new IllegalArgumentException("图书不存在。");
            }
            if (book.getAvailableCopies() >= book.getTotalCopies()) {
                log.error("图书 {} 的可借数量({})已达到或超过总数({})", bookId, book.getAvailableCopies(), book.getTotalCopies());
                throw new IllegalArgumentException("图书库存数据异常，可借数量已达上限。");
            }
            throw new IllegalArgumentException("增加可借数量失败。");
        }

        log.info("图书 {} 可借数量增加", bookId);
    }

    @Transactional
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Book book) {
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getBorrowedCount() > 0) {
            throw new IllegalArgumentException("该图书仍有未归还记录，无法删除。");
        }

        if (!book.getAvailableCopies().equals(book.getTotalCopies())) {
            log.warn("图书 {} 的可借数量({})与总数({})不一致", bookId, book.getAvailableCopies(), book.getTotalCopies());
            throw new IllegalArgumentException("图书库存数据异常，无法删除。");
        }

        bookRepository.deleteById(bookId);
        log.info("图书 {} 已删除", bookId);
    }

    public void validateBook(Book book) {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("图书标题不能为空。");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("图书作者不能为空。");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("图书 ISBN 不能为空。");
        }
        if (book.getLocation() == null || book.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("图书位置不能为空。");
        }
        if (book.getTotalCopies() == null || book.getTotalCopies() < 1) {
            throw new IllegalArgumentException("图书总数必须至少为 1。");
        }
    }

    public void checkRelatedData(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getBorrowedCount() > 0) {
            throw new IllegalArgumentException("该图书仍有未归还记录，无法删除。");
        }

        List<com.library.model.ReservationRecord> activeReservations = reservationRecordRepository.findByBookIdAndStatusInOrderByCreatedAtAsc(
            bookId,
            java.util.Arrays.asList(
                com.library.model.ReservationRecord.ReservationStatus.WAITING,
                com.library.model.ReservationRecord.ReservationStatus.AVAILABLE
            )
        );
        if (!activeReservations.isEmpty()) {
            throw new IllegalArgumentException("该图书仍有活跃预约记录，无法删除。");
        }

        long reviewCount = bookReviewRepository.countByBookId(bookId);
        if (reviewCount > 0) {
            throw new IllegalArgumentException("该图书仍有评论记录，无法删除。");
        }

        // TODO: 需要在 BookFavoriteRepository 中添加 countByBookId 或 existsByBookId 方法
        // 暂时注释掉以避免全表扫描性能问题
        // long favoriteCount = bookFavoriteRepository.countByBookId(bookId);
        // if (favoriteCount > 0) {
        //     throw new IllegalArgumentException("该图书仍有收藏记录，无法删除。");
        // }

        // TODO: 需要在 ReadingStatusRecordRepository 中添加 countByBookId 或 existsByBookId 方法
        // 暂时注释掉以避免全表扫描性能问题
        // long readingStatusCount = readingStatusRecordRepository.countByBookId(bookId);
        // if (readingStatusCount > 0) {
        //     throw new IllegalArgumentException("该图书仍有阅读状态记录，无法删除。");
        // }

        if (!book.getAvailableCopies().equals(book.getTotalCopies())) {
            log.warn("图书 {} 的可借数量({})与总数({})不一致", bookId, book.getAvailableCopies(), book.getTotalCopies());
            throw new IllegalArgumentException("图书库存数据异常，无法删除。");
        }
    }

    public void adjustCopiesOnUpdate(Book existingBook, Book updatedBook) {
        if (updatedBook.getTotalCopies() == null) {
            return;
        }

        Integer newTotalCopies = updatedBook.getTotalCopies();
        Integer borrowedCount = existingBook.getBorrowedCount();

        if (newTotalCopies < borrowedCount) {
            throw new IllegalArgumentException("新的总副本数不能小于已借出数量。");
        }

        Integer newAvailableCopies = newTotalCopies - borrowedCount;
        updatedBook.setAvailableCopies(newAvailableCopies);
        updatedBook.setBorrowedCount(borrowedCount);

        log.info("图书 {} 库存调整：总数 {} -> {}，可借  -> {}，已借 {}",
            existingBook.getId(),
            existingBook.getTotalCopies(), newTotalCopies,
            existingBook.getAvailableCopies(), newAvailableCopies,
            borrowedCount);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchDeleteResponse batchDeleteBooks(List<Long> bookIds) {
        int successCount = 0;
        int failedCount = 0;
        List<BatchOperationFailure> failures = new ArrayList<>();

        log.info("开始批量删除图书，共 {} 本", bookIds.size());

        for (Long bookId : bookIds) {
            try {
                checkRelatedData(bookId);
                deleteBook(bookId);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failures.add(BatchOperationFailure.ofBookId(bookId, e.getMessage()));
                log.warn("删除图书 {} 失败: {}", bookId, e.getMessage());
            }
        }

        log.info("批量删除完成，成功 {} 本，失败 {} 本", successCount, failedCount);
        return new BatchDeleteResponse(successCount, failedCount, failures);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse batchCreateBooks(List<Book> books) {
        int successCount = 0;
        int failedCount = 0;
        List<BatchOperationFailure> failures = new ArrayList<>();
        Set<String> processedIsbns = new HashSet<>();

        log.info("开始批量导入图书，共 {} 本", books.size());

        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            int rowNumber = i + 2; // 行号从 2 开始（第 1 行是表头）

            try {
                // 验证图书数据
                validateBook(book);

                String normalizedIsbn = normalizeIsbn(book.getIsbn());
                if (normalizedIsbn.isEmpty()) {
                    throw new IllegalArgumentException("ISBN is missing after normalization.");
                }

                // 检查当前批次中是否有重复的 ISBN
                if (processedIsbns.contains(normalizedIsbn)) {
                    throw new IllegalArgumentException("ISBN 在当前批次中重复。");
                }

                // 检查数据库中是否已存在该 ISBN
                if (bookRepository.findFirstByNormalizedIsbn(normalizedIsbn).isPresent()) {
                    throw new IllegalArgumentException("ISBN 已存在于数据库中。");
                }

                // 设置初始库存状态
                book.setAvailableCopies(book.getTotalCopies());
                book.setBorrowedCount(0);

                // 创建图书
                createBook(book);
                processedIsbns.add(normalizedIsbn);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failures.add(BatchOperationFailure.ofRow(rowNumber, e.getMessage()));
                log.warn("导入第 {} 行图书失败: {}", rowNumber, e.getMessage());
            }
        }

        log.info("批量导入完成，成功 {} 本，失败 {} 本", successCount, failedCount);
        return new ImportResponse(successCount, failedCount, failures);
    }

    public long countTotalBorrowsByBookId(Long bookId) {
        return borrowRecordRepository.countByBookId(bookId);
    }

    public long countActiveBorrowsByBookId(Long bookId, List<com.library.model.BorrowRecord.BorrowStatus> statuses) {
        return borrowRecordRepository.countByBookIdAndStatusIn(bookId, statuses);
    }

    public List<com.library.model.BorrowRecord> findRecentBorrowRecords(Long bookId, int limit) {
        return borrowRecordRepository.findTop5ByBookIdOrderByCreatedAtDesc(bookId);
    }

    public long countWaitingReservationsByBookId(Long bookId) {
        return reservationRecordRepository.countWaitingReservationsByBookId(bookId);
    }

    public long countAvailableReservationsByBookId(Long bookId) {
        return reservationRecordRepository.countByBookIdAndStatus(
            bookId, com.library.model.ReservationRecord.ReservationStatus.AVAILABLE);
    }
}
