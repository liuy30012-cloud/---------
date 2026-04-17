package com.library.controller;

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
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void addBookTag_requiresAuthentication() {
        // 准备测试数据
        String token = "Bearer valid-token";
        Long bookId = 1L;
        String tagName = "测试标签";
        Long userId = 100L;

        // 模拟 JWT 验证
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(userId);

        // 模拟保存标签
        BookTag savedTag = new BookTag();
        savedTag.setId(1L);
        savedTag.setBookId(bookId);
        savedTag.setTagName(tagName);
        savedTag.setTagType("USER_GENERATED");
        savedTag.setUserId(userId);
        savedTag.setUsageCount(0);

        when(bookTagRepository.save(any(BookTag.class))).thenReturn(savedTag);

        // 调用接口
        ResponseEntity<?> response = controller.addBookTag(token, bookId, tagName, "USER_GENERATED");

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        // 验证 JWT 被调用
        verify(jwtUtil, times(1)).getUserIdFromToken("valid-token");

        // 验证保存时包含了用户 ID
        verify(bookTagRepository, times(1)).save(argThat(tag ->
            tag.getBookId().equals(bookId) &&
            tag.getTagName().equals(tagName) &&
            tag.getUserId().equals(userId)
        ));
    }

    @Test
    void addBookTag_withoutToken_shouldFail() {
        // 测试没有 token 的情况
        // 这个测试应该在实现后验证抛出异常或返回错误
        Long bookId = 1L;
        String tagName = "测试标签";

        // 当没有提供 token 时，应该抛出异常
        assertThrows(Exception.class, () -> {
            controller.addBookTag(null, bookId, tagName, "USER_GENERATED");
        });
    }

    @Test
    void addBookTag_withInvalidToken_shouldFail() {
        // 测试无效 token 的情况
        String invalidToken = "Bearer invalid-token";
        Long bookId = 1L;
        String tagName = "测试标签";

        // 模拟 JWT 验证失败
        when(jwtUtil.getUserIdFromToken("invalid-token"))
            .thenThrow(new IllegalArgumentException("令牌无效"));

        // 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            controller.addBookTag(invalidToken, bookId, tagName, "USER_GENERATED");
        });
    }
}
