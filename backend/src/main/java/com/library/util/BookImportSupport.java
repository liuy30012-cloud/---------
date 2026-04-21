package com.library.util;

import com.library.model.Book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookImportSupport {

    public static final List<String> TEMPLATE_HEADERS = List.of(
        "title",
        "author",
        "isbn",
        "location",
        "coverUrl",
        "status",
        "year",
        "description",
        "languageCode",
        "availability",
        "category",
        "circulationPolicy",
        "totalCopies"
    );

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        normalizeHeader("title"),
        normalizeHeader("author"),
        normalizeHeader("isbn"),
        normalizeHeader("location")
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    private static final String TITLE_HEADER = normalizeHeader("title");
    private static final String AUTHOR_HEADER = normalizeHeader("author");
    private static final String ISBN_HEADER = normalizeHeader("isbn");
    private static final String LOCATION_HEADER = normalizeHeader("location");
    private static final String COVER_URL_HEADER = normalizeHeader("coverUrl");
    private static final String STATUS_HEADER = normalizeHeader("status");
    private static final String YEAR_HEADER = normalizeHeader("year");
    private static final String DESCRIPTION_HEADER = normalizeHeader("description");
    private static final String LANGUAGE_CODE_HEADER = normalizeHeader("languageCode");
    private static final String AVAILABILITY_HEADER = normalizeHeader("availability");
    private static final String CATEGORY_HEADER = normalizeHeader("category");
    private static final String CIRCULATION_POLICY_HEADER = normalizeHeader("circulationPolicy");
    private static final String TOTAL_COPIES_HEADER = normalizeHeader("totalCopies");

    private BookImportSupport() {
    }

    public static Map<String, Integer> buildHeaderIndexes(List<String> rawHeaders) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String normalized = normalizeHeader(rawHeaders.get(i));
            if (!normalized.isEmpty() && !indexes.containsKey(normalized)) {
                indexes.put(normalized, i);
            }
        }
        return indexes;
    }

    public static void validateHeaders(Collection<String> headers) {
        List<String> missing = new ArrayList<>();
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!headers.contains(requiredHeader)) {
                missing.add(requiredHeader);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required headers: " + String.join(", ", missing));
        }
    }

    public static Map<String, String> toRowMap(String[] row, Map<String, Integer> headerIndexes) {
        Map<String, String> rowMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
            int index = entry.getValue();
            String value = index < row.length ? row[index] : "";
            rowMap.put(entry.getKey(), normalizeValue(value));
        }
        return rowMap;
    }

    public static Book parseBook(Map<String, String> rowValues) {
        Book book = new Book();

        book.setTitle(requiredValue(rowValues, TITLE_HEADER));
        book.setAuthor(requiredValue(rowValues, AUTHOR_HEADER));
        book.setIsbn(requiredValue(rowValues, ISBN_HEADER));
        book.setLocation(requiredValue(rowValues, LOCATION_HEADER));
        book.setCoverUrl(optionalValue(rowValues, COVER_URL_HEADER));
        book.setStatus(optionalValue(rowValues, STATUS_HEADER));
        book.setYear(parseYear(optionalValue(rowValues, YEAR_HEADER)));
        book.setDescription(optionalValue(rowValues, DESCRIPTION_HEADER));
        book.setLanguageCode(optionalValue(rowValues, LANGUAGE_CODE_HEADER));
        book.setAvailability(optionalValue(rowValues, AVAILABILITY_HEADER));
        book.setCategory(optionalValue(rowValues, CATEGORY_HEADER));
        book.setCirculationPolicy(parseCirculationPolicy(optionalValue(rowValues, CIRCULATION_POLICY_HEADER)));
        book.setTotalCopies(parseTotalCopies(optionalValue(rowValues, TOTAL_COPIES_HEADER)));
        book.setAvailableCopies(book.getTotalCopies());
        book.setBorrowedCount(0);

        return book;
    }

    public static String normalizeHeader(String rawHeader) {
        if (rawHeader == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char current : rawHeader.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char current : isbn.trim().toUpperCase(Locale.ROOT).toCharArray()) {
            if (Character.isDigit(current) || current == 'X') {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requiredValue(Map<String, String> rowValues, String key) {
        String value = optionalValue(rowValues, key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing required value for column: " + key);
        }
        return value;
    }

    private static String optionalValue(Map<String, String> rowValues, String key) {
        String value = normalizeValue(rowValues.getOrDefault(key, ""));
        return value.isEmpty() ? null : value;
    }

    private static String parseYear(String rawYear) {
        if (rawYear == null || rawYear.isBlank()) {
            return null;
        }
        Matcher matcher = YEAR_PATTERN.matcher(rawYear);
        if (matcher.find()) {
            return matcher.group();
        }
        return rawYear.length() > 20 ? rawYear.substring(0, 20) : rawYear;
    }

    private static int parseTotalCopies(String rawTotalCopies) {
        if (rawTotalCopies == null || rawTotalCopies.isBlank()) {
            return 1;
        }
        try {
            int totalCopies = Integer.parseInt(rawTotalCopies.trim());
            return Math.max(totalCopies, 1);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static Book.CirculationPolicy parseCirculationPolicy(String rawPolicy) {
        if (rawPolicy == null || rawPolicy.isBlank()) {
            return Book.CirculationPolicy.AUTO;
        }

        return switch (rawPolicy.trim().toUpperCase(Locale.ROOT)) {
            case "AUTO", "AVAILABLE", "BORROWABLE", "可借阅" -> Book.CirculationPolicy.AUTO;
            case "MANUAL", "REVIEW", "APPROVAL", "人工审核" -> Book.CirculationPolicy.MANUAL;
            case "REFERENCE_ONLY", "REFERENCE", "READING_ROOM_ONLY", "IN_LIBRARY", "仅馆内阅览" ->
                Book.CirculationPolicy.REFERENCE_ONLY;
            default -> Book.CirculationPolicy.AUTO;
        };
    }
}
