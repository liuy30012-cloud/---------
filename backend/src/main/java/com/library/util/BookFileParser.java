package com.library.util;

import com.library.model.Book;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BookFileParser {

    private static final String[] REQUIRED_HEADERS = {"title", "author", "isbn", "location"};

    public static List<Book> parseExcel(MultipartFile file) throws IOException {
        List<Book> books = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel文件至少需要包含表头和一行数据");
            }

            Row headerRow = sheet.getRow(0);
            validateHeaders(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                Book book = parseExcelRow(row);
                books.add(book);
            }
        }

        return books;
    }

    private static void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel文件缺少表头行");
        }

        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell).toLowerCase().trim();
            headers.add(header);
        }

        for (String required : REQUIRED_HEADERS) {
            if (!headers.contains(required)) {
                throw new IllegalArgumentException("Excel文件缺少必需列: " + required);
            }
        }
    }

    private static Book parseExcelRow(Row row) {
        Book book = new Book();

        book.setTitle(getCellValueAsString(row.getCell(0)));
        book.setAuthor(getCellValueAsString(row.getCell(1)));
        book.setIsbn(getCellValueAsString(row.getCell(2)));
        book.setLocation(getCellValueAsString(row.getCell(3)));

        String circulationPolicy = getCellValueAsString(row.getCell(4));
        book.setCirculationPolicy(circulationPolicy.isEmpty() ? Book.CirculationPolicy.AUTO : Book.CirculationPolicy.valueOf(circulationPolicy));

        String totalCopiesStr = getCellValueAsString(row.getCell(12));
        if (totalCopiesStr != null && !totalCopiesStr.isEmpty()) {
            try {
                book.setTotalCopies(Integer.parseInt(totalCopiesStr));
            } catch (NumberFormatException e) {
                book.setTotalCopies(1);
            }
        } else {
            book.setTotalCopies(1);
        }
        book.setAvailableCopies(book.getTotalCopies());
        book.setBorrowedCount(0);

        return book;
    }

    private static boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (!value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    public static List<Book> parseCsv(MultipartFile file) throws IOException, CsvException {
        List<Book> books = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = csvReader.readAll();

            if (allRows.size() < 2) {
                throw new IllegalArgumentException("CSV文件至少需要包含表头和一行数据");
            }

            String[] headers = allRows.get(0);
            validateCsvHeaders(headers);

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                if (isEmptyCsvRow(row)) {
                    continue;
                }

                Book book = parseCsvRow(row);
                books.add(book);
            }
        }

        return books;
    }

    private static void validateCsvHeaders(String[] headers) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("CSV文件缺少表头行");
        }

        List<String> headerList = new ArrayList<>();
        for (String header : headers) {
            headerList.add(header.toLowerCase().trim());
        }

        for (String required : REQUIRED_HEADERS) {
            if (!headerList.contains(required)) {
                throw new IllegalArgumentException("CSV文件缺少必需列: " + required);
            }
        }
    }

    private static Book parseCsvRow(String[] row) {
        Book book = new Book();

        book.setTitle(getValueOrNull(row, 0));
        book.setAuthor(getValueOrNull(row, 1));
        book.setIsbn(getValueOrNull(row, 2));
        book.setLocation(getValueOrNull(row, 3));

        String circulationPolicy = getValueOrNull(row, 4);
        book.setCirculationPolicy(circulationPolicy == null || circulationPolicy.isEmpty() ? Book.CirculationPolicy.AUTO : Book.CirculationPolicy.valueOf(circulationPolicy));

        String totalCopiesStr = getValueOrNull(row, 12);
        if (totalCopiesStr != null && !totalCopiesStr.isEmpty()) {
            try {
                book.setTotalCopies(Integer.parseInt(totalCopiesStr));
            } catch (NumberFormatException e) {
                book.setTotalCopies(1);
            }
        } else {
            book.setTotalCopies(1);
        }
        book.setAvailableCopies(book.getTotalCopies());
        book.setBorrowedCount(0);

        return book;
    }

    private static boolean isEmptyCsvRow(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }

        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getValueOrNull(String[] row, int index) {
        if (index >= row.length) {
            return "";
        }
        String value = row[index];
        return (value == null || value.trim().isEmpty()) ? "" : value.trim();
    }

    public static byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("图书导入模板");

            // 创建表头行 - 顺序与 parseExcelRow() 方法完全一致
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "title",              // 索引 0
                "author",             // 索引 1
                "isbn",               // 索引 2
                "location",           // 索引 3
                "circulationPolicy",  // 索引 4
                "",                   // 索引 5 (未使用)
                "",                   // 索引 6 (未使用)
                "",                   // 索引 7 (未使用)
                "",                   // 索引 8 (未使用)
                "",                   // 索引 9 (未使用)
                "",                   // 索引 10 (未使用)
                "",                   // 索引 11 (未使用)
                "totalCopies"         // 索引 12
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 创建示例数据行 - 与表头对应
            Row exampleRow = sheet.createRow(1);
            String[] exampleData = {
                "Java编程思想",       // title
                "Bruce Eckel",       // author
                "978-0131872486",    // isbn
                "A区-1层-001",       // location
                "可借阅",            // circulationPolicy
                "",                  // 未使用
                "",                  // 未使用
                "",                  // 未使用
                "",                  // 未使用
                "",                  // 未使用
                "",                  // 未使用
                "",                  // 未使用
                "5"                  // totalCopies
            };

            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(exampleData[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
