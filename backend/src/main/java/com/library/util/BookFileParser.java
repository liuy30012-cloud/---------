package com.library.util;

import com.library.model.Book;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BookFileParser {

    private BookFileParser() {
    }

    public static List<Book> parseExcel(MultipartFile file) throws IOException {
        List<Book> books = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel file must contain a header row and at least one data row.");
            }

            Map<String, Integer> headerIndexes = readExcelHeaderIndexes(
                sheet.getRow(0),
                dataFormatter,
                evaluator
            );

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, dataFormatter, evaluator)) {
                    continue;
                }

                books.add(parseExcelRow(row, headerIndexes, dataFormatter, evaluator));
            }
        }

        return books;
    }

    public static List<Book> parseCsv(MultipartFile file) throws IOException, CsvException {
        List<Book> books = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                throw new IllegalArgumentException("CSV file must contain a header row.");
            }

            Map<String, Integer> headerIndexes = BookImportSupport.buildHeaderIndexes(Arrays.asList(headerRow));
            BookImportSupport.validateHeaders(headerIndexes.keySet());

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                if (isEmptyCsvRow(row)) {
                    continue;
                }

                books.add(BookImportSupport.parseBook(BookImportSupport.toRowMap(row, headerIndexes)));
            }
        }

        return books;
    }

    public static byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("book_import_template");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < BookImportSupport.TEMPLATE_HEADERS.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(BookImportSupport.TEMPLATE_HEADERS.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6_000);
            }

            Row exampleRow = sheet.createRow(1);
            String[] exampleData = {
                "Thinking in Java",
                "Bruce Eckel",
                "9780131872486",
                "A区>1层>计算机>001",
                "https://covers.openlibrary.org/b/olid/OL7440033M-M.jpg?default=false",
                "AVAILABLE",
                "2006",
                "Classic introduction to Java programming.",
                "en",
                "Available",
                "计算机",
                "AUTO",
                "5"
            };

            for (int i = 0; i < exampleData.length; i++) {
                exampleRow.createCell(i).setCellValue(exampleData[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static Map<String, Integer> readExcelHeaderIndexes(
        Row headerRow,
        DataFormatter dataFormatter,
        FormulaEvaluator evaluator
    ) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel file is missing a header row.");
        }

        List<String> headers = new ArrayList<>();
        int cellCount = Math.max(headerRow.getLastCellNum(), 0);
        for (int i = 0; i < cellCount; i++) {
            headers.add(getCellValueAsString(headerRow.getCell(i), dataFormatter, evaluator));
        }

        Map<String, Integer> headerIndexes = BookImportSupport.buildHeaderIndexes(headers);
        BookImportSupport.validateHeaders(headerIndexes.keySet());
        return headerIndexes;
    }

    private static Book parseExcelRow(
        Row row,
        Map<String, Integer> headerIndexes,
        DataFormatter dataFormatter,
        FormulaEvaluator evaluator
    ) {
        Map<String, String> rowValues = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
            rowValues.put(
                entry.getKey(),
                getCellValueAsString(row.getCell(entry.getValue()), dataFormatter, evaluator)
            );
        }
        return BookImportSupport.parseBook(rowValues);
    }

    private static boolean isEmptyRow(Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        int lastCellNum = Math.max(row.getLastCellNum(), 0);
        for (int i = 0; i < lastCellNum; i++) {
            if (!getCellValueAsString(row.getCell(i), dataFormatter, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
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

    private static String getCellValueAsString(Cell cell, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell, evaluator).trim();
    }
}
