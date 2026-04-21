package com.library.service.elasticsearch;

import com.library.document.BookDocument;
import com.library.model.Book;
import com.library.repository.BookDocumentRepository;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    matchIfMissing = false
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
            log.info("Successfully synchronized book {} to Elasticsearch.", book.getId());
        } catch (Exception e) {
            log.error("Failed to synchronize book {} to Elasticsearch.", book.getId(), e);
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
            log.info("Successfully removed book {} from Elasticsearch.", bookId);
        } catch (Exception e) {
            log.error("Failed to remove book {} from Elasticsearch.", bookId, e);
        }
    }

    @Transactional
    public void syncAllBooks() {
        log.info("Starting full book synchronization to Elasticsearch.");

        int pageNumber = 0;
        int totalSynced = 0;
        Page<Book> page;

        do {
            page = bookRepository.findAll(PageRequest.of(pageNumber, 1_000));
            if (!page.isEmpty()) {
                List<BookDocument> docs = page.getContent().stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());
                bookDocumentRepository.saveAll(docs);
                totalSynced += docs.size();
            }
            pageNumber++;
        } while (page.hasNext());

        log.info("Completed full Elasticsearch sync for {} books.", totalSynced);
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
