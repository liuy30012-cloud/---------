package com.library.util;

import com.library.model.Book;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookFileParserTest {

    @Test
    void parseCsvParsesAllSupportedFieldsWithShuffledHeaders() throws IOException, CsvException {
        String csv = String.join("\n",
            "totalCopies,description,title,location,isbn,author,coverUrl,status,year,languageCode,availability,category,circulationPolicy",
            "6,Deep dive into distributed systems,Designing Data-Intensive Applications,A区>2层>计算机>018,9781449373320,Martin Kleppmann,https://covers.openlibrary.org/b/olid/OL1M-M.jpg?default=false,AVAILABLE,2017,en,Available,计算机,MANUAL"
        );

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "books.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        List<Book> books = BookFileParser.parseCsv(file);

        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Designing Data-Intensive Applications", book.getTitle());
        assertEquals("Martin Kleppmann", book.getAuthor());
        assertEquals("9781449373320", book.getIsbn());
        assertEquals("A区>2层>计算机>018", book.getLocation());
        assertEquals("https://covers.openlibrary.org/b/olid/OL1M-M.jpg?default=false", book.getCoverUrl());
        assertEquals("AVAILABLE", book.getStatus());
        assertEquals("2017", book.getYear());
        assertEquals("Deep dive into distributed systems", book.getDescription());
        assertEquals("en", book.getLanguageCode());
        assertEquals("Available", book.getAvailability());
        assertEquals("计算机", book.getCategory());
        assertEquals(Book.CirculationPolicy.MANUAL, book.getCirculationPolicy());
        assertEquals(6, book.getTotalCopies());
    }

    @Test
    void parseExcelAllowsMissingOptionalHeaders() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("books");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("location");
            header.createCell(1).setCellValue("title");
            header.createCell(2).setCellValue("isbn");
            header.createCell(3).setCellValue("author");
            header.createCell(4).setCellValue("totalCopies");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("B区>4层>文学>032");
            row.createCell(1).setCellValue("One Hundred Years of Solitude");
            row.createCell(2).setCellValue("9780060883287");
            row.createCell(3).setCellValue("Gabriel Garcia Marquez");
            row.createCell(4).setCellValue(7);

            workbook.write(out);
            workbookBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "books.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            workbookBytes
        );

        List<Book> books = BookFileParser.parseExcel(file);

        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("One Hundred Years of Solitude", book.getTitle());
        assertEquals("Gabriel Garcia Marquez", book.getAuthor());
        assertEquals("9780060883287", book.getIsbn());
        assertEquals("B区>4层>文学>032", book.getLocation());
        assertEquals(7, book.getTotalCopies());
        assertEquals(Book.CirculationPolicy.AUTO, book.getCirculationPolicy());
        assertNull(book.getCoverUrl());
        assertNull(book.getCategory());
        assertNull(book.getLanguageCode());
    }

    @Test
    void parseCsvFailsWhenRequiredHeadersAreMissing() {
        String csv = String.join("\n",
            "title,isbn,location,totalCopies",
            "Missing Author Book,9780000000001,A区>1层>综合>001,2"
        );

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "broken.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BookFileParser.parseCsv(file)
        );

        assertEquals("Missing required headers: author", exception.getMessage());
    }
}
