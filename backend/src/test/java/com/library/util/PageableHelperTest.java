package com.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageableHelperTest {

    @Test
    void createPageableSupportsSplitSortTokens() {
        Pageable pageable = PageableHelper.createPageable(0, 20, 20, new String[]{"createdAt", "desc"});

        List<Sort.Order> orders = pageable.getSort().toList();
        assertEquals(1, orders.size());
        assertEquals("createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    void createPageableSupportsCombinedAndRepeatedSortTokens() {
        Pageable pageable = PageableHelper.createPageable(0, 20, 20, new String[]{"createdAt,desc", "updatedAt", "asc"});

        List<Sort.Order> orders = pageable.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals("createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("updatedAt", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }
}
