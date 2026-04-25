package com.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class PageableHelperTest {

    @Test
    void createPageable_withValidParams_shouldCreateCorrectPageable() {
        Pageable pageable = PageableHelper.createPageable(0, 20, 10, new String[]{"createdAt,desc"});

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void createPageable_withNegativePage_shouldNormalizeToZero() {
        Pageable pageable = PageableHelper.createPageable(-1, 20, 10, new String[]{});

        assertEquals(0, pageable.getPageNumber());
    }

    @Test
    void createPageable_withZeroSize_shouldUseDefaultSize() {
        Pageable pageable = PageableHelper.createPageable(0, 0, 15, new String[]{});

        assertEquals(15, pageable.getPageSize());
    }

    @Test
    void createPageable_withSizeOver100_shouldCapAt100() {
        Pageable pageable = PageableHelper.createPageable(0, 200, 10, new String[]{});

        assertEquals(100, pageable.getPageSize());
    }
}
