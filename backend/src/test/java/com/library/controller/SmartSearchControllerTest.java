package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.model.BookTag;
import com.library.repository.BookTagRepository;
import com.library.service.BookRecommendationService;
import com.library.service.SmartSearchService;
import com.library.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartSearchControllerTest {

    private AutoCloseable mocks;
    private SmartSearchController controller;

    @Mock
    private SmartSearchService smartSearchService;

    @Mock
    private BookRecommendationService recommendationService;

    @Mock
    private BookTagRepository bookTagRepository;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new SmartSearchController(
            smartSearchService,
            recommendationService,
            bookTagRepository,
            jwtUtil
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void addBookTag_requiresAuthenticationToken() {
        Long bookId = 1L;
        String tagName = "测试标签";
        Long userId = 100L;
        Authentication authentication = new TestingAuthenticationToken("user", "credentials");

        when(jwtUtil.getUserIdFromToken(any())).thenReturn(userId);

        BookTag savedTag = new BookTag();
        savedTag.setId(1L);
        savedTag.setBookId(bookId);
        savedTag.setTagName(tagName);
        savedTag.setTagType("USER_GENERATED");
        savedTag.setUserId(userId);
        savedTag.setUsageCount(0);
        when(bookTagRepository.save(any(BookTag.class))).thenReturn(savedTag);

        ResponseEntity<?> response = controller.addBookTag(authentication, bookId, tagName, "USER_GENERATED");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(bookTagRepository, times(1)).save(argThat(tag ->
            tag.getBookId().equals(bookId)
                && tag.getTagName().equals(tagName)
                && tag.getUserId().equals(userId)
        ));
    }

    @Test
    void addBookTag_withoutAuthentication_shouldFail() {
        assertThrows(Exception.class, () -> controller.addBookTag(null, 1L, "测试标签", "USER_GENERATED"));
    }

    @Test
    void addBookTag_withInvalidToken_shouldFail() {
        Authentication authentication = new TestingAuthenticationToken("user", "credentials");
        when(jwtUtil.getUserIdFromToken(any())).thenThrow(new IllegalArgumentException("令牌无效"));

        assertThrows(IllegalArgumentException.class, () ->
            controller.addBookTag(authentication, 1L, "测试标签", "USER_GENERATED")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void smartSearch_includesSearchEngineInResponse() {
        SmartSearchService.SmartSearchResult result = new SmartSearchService.SmartSearchResult();
        result.setBooks(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 12), 0));
        result.setOriginalQuery("设计模式");
        result.setNormalizedQuery("设计模式");
        result.setSearchEngine(SmartSearchService.SearchEngine.ELASTICSEARCH);

        when(smartSearchService.smartSearch(eq("设计模式"), any())).thenReturn(result);

        ResponseEntity<?> response = controller.smartSearch("设计模式", 0, 12);
        ApiResponse<Map<String, Object>> apiResponse = (ApiResponse<Map<String, Object>>) response.getBody();

        assertNotNull(apiResponse);
        assertEquals("ELASTICSEARCH", apiResponse.getData().get("searchEngine"));
    }
}
