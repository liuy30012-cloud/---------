package com.library.config;

import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "app.demo-books.enabled=true",
    "app.demo-books.seed-if-empty-only=true",
    "app.demo-books.resource=classpath:/demo/test-demo-books.csv",
    "app.demo-books.target-count=20",
    "library.search.elasticsearch.enabled=false"
})
@ActiveProfiles("test")
@Import({
    TestElasticsearchConfig.class,
    DemoBooksInitializerSkipsWhenNotEmptyIntegrationTest.PreloadConfig.class
})
@EnableAutoConfiguration(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class
})
class DemoBooksInitializerSkipsWhenNotEmptyIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ElasticsearchSyncService elasticsearchSyncService;

    @Test
    void skipsSeedingWhenCatalogAlreadyHasData() {
        assertEquals(1, bookRepository.count());
        verify(elasticsearchSyncService, never()).syncAllBooks();
    }

    @TestConfiguration
    static class PreloadConfig {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        ApplicationRunner preloadBookRunner(BookRepository bookRepository) {
            return args -> {
                Book book = new Book();
                book.setTitle("Existing Catalog Entry");
                book.setAuthor("Existing Author");
                book.setIsbn("9780000000000");
                book.setLocation("A区>1层>综合>000");
                book.setCategory("综合");
                book.setLanguageCode("en");
                book.setCirculationPolicy(Book.CirculationPolicy.AUTO);
                book.setTotalCopies(1);
                book.setAvailableCopies(1);
                book.setBorrowedCount(0);
                bookRepository.save(book);
            };
        }
    }
}
