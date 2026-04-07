package com.library.controller;

import com.library.dto.SearchHistoryRequest;
import com.library.model.SearchHistoryRecord;
import com.library.service.SearchHistoryService;
import com.library.util.JwtUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SearchHistoryControllerTest {

    private AutoCloseable mocks;
    private SearchHistoryController controller;
    private LocalValidatorFactoryBean validator;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SearchHistoryService searchHistoryService;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new SearchHistoryController(searchHistoryService, jwtUtil);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        when(jwtUtil.getUserIdFromToken("mock-token")).thenReturn(1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        validator.close();
        mocks.close();
    }

    @Test
    void addHistoryRejectsBlankKeyword() {
        SearchHistoryRequest request = new SearchHistoryRequest();
        request.setKeyword("   ");

        Set<ConstraintViolation<SearchHistoryRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(violation -> "搜索关键词不能为空".equals(violation.getMessage())));
    }

    @Test
    void addHistoryTrimsKeywordAndDefaultsResultCount() {
        SearchHistoryRequest request = new SearchHistoryRequest();
        request.setKeyword("  java  ");
        request.setResultCount(null);

        SearchHistoryRecord savedRecord = new SearchHistoryRecord();
        savedRecord.setKeyword("java");
        savedRecord.setResultCount(0);
        when(searchHistoryService.saveSearch(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(SearchHistoryRequest.class)))
            .thenReturn(savedRecord);

        SearchHistoryRecord record = controller.addHistory(
            request,
            new TestingAuthenticationToken("user", "mock-token")
        );

        assertEquals("java", record.getKeyword());
        assertEquals(0, record.getResultCount());
    }
}
