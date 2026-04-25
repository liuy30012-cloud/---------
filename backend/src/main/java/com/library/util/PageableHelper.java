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
        for (int i = 0; i < sort.length; i++) {
            String sortParam = sort[i];
            if (sortParam == null) {
                continue;
            }

            String[] parts = sortParam.split(",");
            if (parts.length == 0) {
                continue;
            }

            String field = parts[0].trim();
            if (field.isEmpty()) {
                continue;
            }

            String directionToken = null;
            if (parts.length > 1) {
                directionToken = parts[1].trim();
            } else if (i + 1 < sort.length && isDirectionToken(sort[i + 1])) {
                directionToken = sort[++i].trim();
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(directionToken)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, field));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private static boolean isDirectionToken(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim();
        return "asc".equalsIgnoreCase(normalized) || "desc".equalsIgnoreCase(normalized);
    }
}
