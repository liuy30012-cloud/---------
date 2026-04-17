package com.library.service.elasticsearch;

import com.library.document.BookDocument;
import com.library.model.Book;
import com.library.repository.BookDocumentRepository;
import com.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSyncServiceTest {

    @Mock
    private BookDocumentRepository bookDocumentRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ElasticsearchSyncService syncService;

    @Test
    void testIndexBook() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("测试图书");
        book.setAuthor("测试作者");

        syncService.indexBook(book);

        verify(bookDocumentRepository, times(1)).save(any(BookDocument.class));
    }

    @Test
    void testDeleteBook() {
        Long bookId = 1L;

        syncService.deleteBook(bookId);

        verify(bookDocumentRepository, times(1)).deleteById(bookId);
    }
}
