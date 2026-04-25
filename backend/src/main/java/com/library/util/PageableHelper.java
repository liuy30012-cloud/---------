package com.library.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageableHelper {

    private PageableHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pageable createPageable(int page, int size, int defaultSize, String[] sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? Math.min(size, 100) : defaultSize;
        Sort sortObj = parseSort(sort);
        return PageRequest.of(normalizedPage, normalizedSize, sortObj);
    }

    private static Sort parseSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            if (parts.length >= 1) {
                String field = parts[0].trim();
                Sort.Direction direction = parts.length > 1 &&
                    "asc".equalsIgnoreCase(parts[1].trim())
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, field));
            }
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
