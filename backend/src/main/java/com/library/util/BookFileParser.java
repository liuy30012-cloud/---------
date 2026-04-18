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

    // __CONTINUE_HERE__
}
