package com.library.config;

import com.library.repository.BookRepository;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "app.demo-books.enabled=true",
    "app.demo-books.seed-if-empty-only=true",
    "app.demo-books.resource=classpath:/demo/test-demo-books.csv",
    "app.demo-books.target-count=20",
    "app.demo-books.disable-es-listener-during-seed=true",
    "library.search.elasticsearch.enabled=false"
})
@ActiveProfiles("test")
@Import(TestElasticsearchConfig.class)
@EnableAutoConfiguration(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class
})
class DemoBooksInitializerSeedsWhenEmptyIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ElasticsearchSyncService elasticsearchSyncService;

    @Test
    void seedsDemoBooksAndRunsSingleFullSync() {
        assertEquals(20, bookRepository.count());
        assertTrue(bookRepository.findDistinctCategories().size() >= 1);
        assertTrue(bookRepository.findDistinctLanguages().size() >= 1);

        verify(elasticsearchSyncService).syncAllBooks();
        verify(elasticsearchSyncService, never()).indexBook(any());
    }
}
