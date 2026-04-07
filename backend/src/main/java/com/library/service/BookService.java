package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElse(null);
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
}
