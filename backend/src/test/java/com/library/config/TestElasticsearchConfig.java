package com.library.config;

import com.library.repository.BookDocumentRepository;
import com.library.service.elasticsearch.ElasticsearchSearchService;
import com.library.service.elasticsearch.ElasticsearchStatisticsService;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestElasticsearchConfig {

    @Bean
    @Primary
    public ElasticsearchSyncService elasticsearchSyncService() {
        return mock(ElasticsearchSyncService.class);
    }

    @Bean
    @Primary
    public ElasticsearchSearchService elasticsearchSearchService() {
        return mock(ElasticsearchSearchService.class);
    }

    @Bean
    @Primary
    public ElasticsearchStatisticsService elasticsearchStatisticsService() {
        return mock(ElasticsearchStatisticsService.class);
    }

    @Bean
    @Primary
    public BookDocumentRepository bookDocumentRepository() {
        return mock(BookDocumentRepository.class);
    }

    @Bean(name = {"elasticsearchTemplate", "elasticsearchOperations"})
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        return mock(ElasticsearchOperations.class);
    }
}
