package com.library.service.elasticsearch;

import com.library.document.BookDocument;
import com.library.model.Book;
import com.library.repository.BookDocumentRepository;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "library.search.elasticsearch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ElasticsearchSyncService {

    private final BookDocumentRepository bookDocumentRepository;
    private final BookRepository bookRepository;

    @Async
    @Transactional
    public void indexBook(Book book) {
        try {
            BookDocument doc = convertToDocument(book);
            bookDocumentRepository.save(doc);
            log.info("成功同步图书到 ES: {}", book.getId());
        } catch (Exception e) {
            log.error("同步图书到 ES 失败: {}", book.getId(), e);
        }
    }

    @Async
    @Transactional
    public void updateBook(Book book) {
        indexBook(book);
    }

    @Async
    @Transactional
    public void deleteBook(Long bookId) {
        try {
            bookDocumentRepository.deleteById(bookId);
            log.info("成功从 ES 删除图书: {}", bookId);
        } catch (Exception e) {
            log.error("从 ES 删除图书失败: {}", bookId, e);
        }
    }

    @Transactional
    public void syncAllBooks() {
        log.info("开始全量同步图书到 ES");
        List<Book> books = bookRepository.findAll();
        List<BookDocument> docs = books.stream()
            .map(this::convertToDocument)
            .collect(Collectors.toList());
        bookDocumentRepository.saveAll(docs);
        log.info("全量同步完成，共同步 {} 本图书", docs.size());
    }

    private BookDocument convertToDocument(Book book) {
        BookDocument doc = new BookDocument();
        doc.setId(book.getId());
        doc.setTitle(book.getTitle());
        doc.setAuthor(book.getAuthor());
        doc.setIsbn(book.getIsbn());
        doc.setCategory(book.getCategory());
        doc.setLanguageCode(book.getLanguageCode());
        doc.setTotalCopies(book.getTotalCopies());
        doc.setAvailableCopies(book.getAvailableCopies());
        doc.setBorrowedCount(book.getBorrowedCount());
        doc.setYear(book.getYear());
        doc.setDescription(book.getDescription());
        doc.setCreatedAt(book.getCreatedAt());
        doc.setUpdatedAt(book.getUpdatedAt());
        return doc;
    }
}
