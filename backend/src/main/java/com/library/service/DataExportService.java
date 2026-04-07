package com.library.service;

import com.library.model.BorrowRecord;
import com.library.model.BookReview;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.BookReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookReviewRepository bookReviewRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportBorrowHistoryToExcel(Long userId) throws IOException {
        List<BorrowRecord> records = borrowRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("借阅历史");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"图书标题", "ISBN", "借出日期", "到期日期", "归还日期", "状态", "续借次数", "逾期天数", "罚款金额", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (BorrowRecord record : records) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, record.getBookTitle(), dataStyle);
                createCell(row, 1, record.getBookIsbn(), dataStyle);
                createCell(row, 2, formatDateTime(record.getBorrowDate()), dataStyle);
                createCell(row, 3, formatDateTime(record.getDueDate()), dataStyle);
                createCell(row, 4, formatDateTime(record.getReturnDate()), dataStyle);
                createCell(row, 5, getStatusText(record.getStatus()), dataStyle);
                createCell(row, 6, String.valueOf(record.getRenewCount()), dataStyle);
                createCell(row, 7, String.valueOf(record.getOverdueDays()), dataStyle);
                createCell(row, 8, String.format("%.2f", record.getFineAmount()), dataStyle);
                createCell(row, 9, record.getNotes() != null ? record.getNotes() : "", dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportBookReviewsToExcel(Long userId) throws IOException {
        List<BookReview> reviews = bookReviewRepository.findByUserIdOrderByCreatedAtDesc(userId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("我的评价");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"图书ID", "评分", "评价内容", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (BookReview review : reviews) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, String.valueOf(review.getBookId()), dataStyle);
                createCell(row, 1, String.valueOf(review.getRating()), dataStyle);
                createCell(row, 2, review.getContent() != null ? review.getContent() : "", dataStyle);
                createCell(row, 3, formatDateTime(review.getCreatedAt()), dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public String exportBorrowHistoryToJson(Long userId) {
        List<BorrowRecord> records = borrowRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"borrowHistory\": [\n");

        for (int i = 0; i < records.size(); i++) {
            BorrowRecord record = records.get(i);
            json.append("    {\n");
            json.append("      \"bookTitle\": \"").append(escapeJson(record.getBookTitle())).append("\",\n");
            json.append("      \"bookIsbn\": \"").append(escapeJson(record.getBookIsbn())).append("\",\n");
            json.append("      \"borrowDate\": \"").append(formatDateTime(record.getBorrowDate())).append("\",\n");
            json.append("      \"dueDate\": \"").append(formatDateTime(record.getDueDate())).append("\",\n");
            json.append("      \"returnDate\": \"").append(formatDateTime(record.getReturnDate())).append("\",\n");
            json.append("      \"status\": \"").append(getStatusText(record.getStatus())).append("\",\n");
            json.append("      \"renewCount\": ").append(record.getRenewCount()).append(",\n");
            json.append("      \"overdueDays\": ").append(record.getOverdueDays()).append(",\n");
            json.append("      \"fineAmount\": ").append(record.getFineAmount()).append(",\n");
            json.append("      \"notes\": \"").append(escapeJson(record.getNotes())).append("\"\n");
            json.append("    }");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n}");
        return json.toString();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "-";
        }
        try {
            return ((java.time.LocalDateTime) dateTime).format(DATE_FORMATTER);
        } catch (Exception e) {
            return dateTime.toString();
        }
    }

    private String getStatusText(Object status) {
        if (status == null) {
            return "未知";
        }
        String statusStr = status.toString();
        switch (statusStr) {
            case "PENDING": return "待审批";
            case "APPROVED": return "待取书";
            case "BORROWED": return "借阅中";
            case "RETURNED": return "已归还";
            case "OVERDUE": return "已逾期";
            case "REJECTED": return "未通过";
            default: return statusStr;
        }
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
