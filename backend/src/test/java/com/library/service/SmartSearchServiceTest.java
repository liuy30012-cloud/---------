package com.library.service;

import com.library.document.BookDocument;
import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.repository.SearchSuggestionRepository;
import com.library.service.elasticsearch.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartSearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SearchSuggestionRepository suggestionRepository;

    @Mock
    private FuzzySearchService fuzzySearchService;

    @Mock
    private ElasticsearchSearchService elasticsearchSearchService;

    private SmartSearchService smartSearchService;

    @BeforeEach
    void setUp() {
        smartSearchService = new SmartSearchService(
            bookRepository,
            suggestionRepository,
            fuzzySearchService,
            elasticsearchSearchService
        );
        ReflectionTestUtils.setField(smartSearchService, "elasticsearchEnabled", true);
    }

    @Test
    void smartSearch_usesElasticsearchAndPreservesHitOrder() {
        PageRequest pageable = PageRequest.of(0, 12);
        BookDocument second = new BookDocument();
        second.setId(2L);
        second.setTitle("设计模式");
        BookDocument first = new BookDocument();
        first.setId(1L);
        first.setTitle("深入理解计算机系统");

        when(elasticsearchSearchService.isAvailable()).thenReturn(true);
        when(elasticsearchSearchService.search("设计模式", pageable))
            .thenReturn(new PageImpl<>(List.of(second, first), pageable, 2));

        Book firstBook = new Book();
        firstBook.setId(1L);
        firstBook.setTitle("深入理解计算机系统");
        Book secondBook = new Book();
        secondBook.setId(2L);
        secondBook.setTitle("设计模式");
        when(bookRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(firstBook, secondBook));

        SmartSearchService.SmartSearchResult result = smartSearchService.smartSearch("设计模式", pageable);

        assertEquals(SmartSearchService.SearchEngine.ELASTICSEARCH, result.getSearchEngine());
        assertEquals(2, result.getBooks().getTotalElements());
        assertIterableEquals(List.of(2L, 1L), result.getBooks().getContent().stream().map(Book::getId).toList());
        verify(bookRepository, never()).searchCatalog(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void smartSearch_fallsBackToMysqlWhenElasticsearchFails() {
        PageRequest pageable = PageRequest.of(0, 12);
        when(elasticsearchSearchService.isAvailable()).thenReturn(true);
        when(elasticsearchSearchService.search("设计模式", pageable))
            .thenThrow(new RuntimeException("boom"));

        Book fallbackBook = new Book();
        fallbackBook.setId(9L);
        fallbackBook.setTitle("设计模式");
        Page<Book> fallbackPage = new PageImpl<>(List.of(fallbackBook), pageable, 1);
        when(bookRepository.searchCatalog("设计模式", "", "", "", "", "", pageable)).thenReturn(fallbackPage);

        SmartSearchService.SmartSearchResult result = smartSearchService.smartSearch("设计模式", pageable);

        assertEquals(SmartSearchService.SearchEngine.MYSQL_FALLBACK, result.getSearchEngine());
        assertEquals(1, result.getBooks().getTotalElements());
        verify(bookRepository).searchCatalog("设计模式", "", "", "", "", "", pageable);
    }
}
