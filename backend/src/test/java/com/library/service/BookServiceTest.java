package com.library.service;

import com.library.model.Book;
import com.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReservationRecordRepository reservationRecordRepository;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookFavoriteRepository bookFavoriteRepository;

    @Mock
    private ReadingStatusRecordRepository readingStatusRecordRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("978-1234567890");
        testBook.setLocation("A1-001");
        testBook.setTotalCopies(5);
        testBook.setAvailableCopies(5);
        testBook.setBorrowedCount(0);
        testBook.setCirculationPolicy(Book.CirculationPolicy.AUTO);
    }

    @Test
    void testValidateBook_Success() {
        assertDoesNotThrow(() -> bookService.validateBook(testBook));
    }

    @Test
    void testValidateBook_MissingTitle() {
        testBook.setTitle(null);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("图书标题不能为空。", exception.getMessage());
    }

    @Test
    void testValidateBook_MissingAuthor() {
        testBook.setAuthor("");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("图书作者不能为空。", exception.getMessage());
    }

    @Test
    void testValidateBook_MissingIsbn() {
        testBook.setIsbn(null);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("图书 ISBN 不能为空。", exception.getMessage());
    }

    @Test
    void testValidateBook_InvalidTotalCopies() {
        testBook.setTotalCopies(0);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("图书总数必须至少为 1。", exception.getMessage());
    }

    @Test
    void testAdjustCopiesOnUpdate_Success() {
        Book existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTotalCopies(5);
        existingBook.setAvailableCopies(3);
        existingBook.setBorrowedCount(2);

        Book updatedBook = new Book();
        updatedBook.setTotalCopies(10);

        bookService.adjustCopiesOnUpdate(existingBook, updatedBook);

        assertEquals(10, updatedBook.getTotalCopies());
        assertEquals(8, updatedBook.getAvailableCopies());
        assertEquals(2, updatedBook.getBorrowedCount());
    }

    @Test
    void testAdjustCopiesOnUpdate_TotalCopiesLessThanBorrowed() {
        Book existingBook = new Book();
        existingBook.setBorrowedCount(5);

        Book updatedBook = new Book();
        updatedBook.setTotalCopies(3);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.adjustCopiesOnUpdate(existingBook, updatedBook)
        );
        assertTrue(exception.getMessage().contains("不能小于已借出数量"));
    }
}
