# 图书管理 API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为管理员提供完整的图书管理 API，包括单个图书的增删改、批量删除、批量导入（CSV/Excel）功能

**Architecture:** 在现有 BookController 和 BookService 基础上扩展，添加管理员专用的 POST/PUT/DELETE 端点。使用 DTO 进行数据验证，Service 层处理业务逻辑和关联数据检查，工具类负责文件解析。依赖现有的 BookSyncListener 自动同步 Elasticsearch。

**Tech Stack:** Spring Boot, Spring Security, JPA, Bean Validation, Apache POI (Excel), OpenCSV (CSV)

---

## 文件结构

**新增文件：**
- `backend/src/main/java/com/library/dto/CreateBookRequest.java` - 创建图书请求 DTO
- `backend/src/main/java/com/library/dto/UpdateBookRequest.java` - 更新图书请求 DTO
- `backend/src/main/java/com/library/dto/BatchDeleteRequest.java` - 批量删除请求 DTO
- `backend/src/main/java/com/library/dto/BatchDeleteResponse.java` - 批量删除响应 DTO
- `backend/src/main/java/com/library/dto/ImportResponse.java` - 导入响应 DTO
- `backend/src/main/java/com/library/dto/BatchOperationFailure.java` - 批量操作失败详情 DTO
- `backend/src/main/java/com/library/util/BookFileParser.java` - 文件解析工具类
- `backend/src/test/java/com/library/service/BookServiceTest.java` - BookService 单元测试
- `backend/src/test/java/com/library/controller/BookControllerAdminTest.java` - 管理接口集成测试
- `backend/src/test/java/com/library/util/BookFileParserTest.java` - 文件解析工具测试

**修改文件：**
- `backend/src/main/java/com/library/service/BookService.java` - 添加验证、批量操作方法
- `backend/src/main/java/com/library/controller/BookController.java` - 添加管理接口
- `backend/pom.xml` - 添加 OpenCSV 依赖

---

## Task 1: 添加 OpenCSV 依赖

**Files:**

- Modify: `backend/pom.xml`

- [ ] **Step 1: 添加 OpenCSV 依赖到 pom.xml**

在 `<dependencies>` 部分添加：

```xml
<!-- OpenCSV for CSV parsing -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.7.1</version>
</dependency>
```

- [ ] **Step 2: 更新 Maven 依赖**

Run: `mvn clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/pom.xml
git commit -m "build: 添加 OpenCSV 依赖用于 CSV 文件解析"
```

---

## Task 2: 创建 DTO 类 - BatchOperationFailure

**Files:**

- Create: `backend/src/main/java/com/library/dto/BatchOperationFailure.java`

- [ ] **Step 1: 创建 BatchOperationFailure 类**

```java
package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationFailure {
    private Long bookId;
    private Integer row;
    private String reason;

    public BatchOperationFailure(Long bookId, String reason) {
        this.bookId = bookId;
        this.reason = reason;
    }

    public BatchOperationFailure(Integer row, String reason) {
        this.row = row;
        this.reason = reason;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/dto/BatchOperationFailure.java
git commit -m "feat: 添加批量操作失败详情 DTO"
```

---

## Task 3: 创建 DTO 类 - CreateBookRequest

**Files:**

- Create: `backend/src/main/java/com/library/dto/CreateBookRequest.java`

- [ ] **Step 1: 创建 CreateBookRequest 类**

```java
package com.library.dto;

import com.library.model.Book;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBookRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100字符")
    private String author;

    @NotBlank(message = "ISBN不能为空")
    @Size(max = 50, message = "ISBN长度不能超过50字符")
    private String isbn;

    @NotBlank(message = "位置不能为空")
    @Size(max = 100, message = "位置长度不能超过100字符")
    private String location;

    @Size(max = 500, message = "封面URL长度不能超过500字符")
    private String coverUrl;

    @Size(max = 50, message = "状态长度不能超过50字符")
    private String status;

    @Size(max = 20, message = "出版年份长度不能超过20字符")
    private String year;

    private String description;

    @Size(max = 10, message = "语言代码长度不能超过10字符")
    private String languageCode;

    @Size(max = 50, message = "可用性长度不能超过50字符")
    private String availability;

    @Size(max = 100, message = "分类长度不能超过100字符")
    private String category;

    private Book.CirculationPolicy circulationPolicy = Book.CirculationPolicy.AUTO;

    @Min(value = 1, message = "总副本数必须至少为1")
    private Integer totalCopies = 1;
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/dto/CreateBookRequest.java
git commit -m "feat: 添加创建图书请求 DTO"
```

---

## Task 4: 创建 DTO 类 - UpdateBookRequest

**Files:**

- Create: `backend/src/main/java/com/library/dto/UpdateBookRequest.java`

- [ ] **Step 1: 创建 UpdateBookRequest 类**

```java
package com.library.dto;

import com.library.model.Book;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBookRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100字符")
    private String author;

    @NotBlank(message = "ISBN不能为空")
    @Size(max = 50, message = "ISBN长度不能超过50字符")
    private String isbn;

    @NotBlank(message = "位置不能为空")
    @Size(max = 100, message = "位置长度不能超过100字符")
    private String location;

    @Size(max = 500, message = "封面URL长度不能超过500字符")
    private String coverUrl;

    @Size(max = 50, message = "状态长度不能超过50字符")
    private String status;

    @Size(max = 20, message = "出版年份长度不能超过20字符")
    private String year;

    private String description;

    @Size(max = 10, message = "语言代码长度不能超过10字符")
    private String languageCode;

    @Size(max = 50, message = "可用性长度不能超过50字符")
    private String availability;

    @Size(max = 100, message = "分类长度不能超过100字符")
    private String category;

    private Book.CirculationPolicy circulationPolicy;

    @Min(value = 1, message = "总副本数必须至少为1")
    private Integer totalCopies;
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/dto/UpdateBookRequest.java
git commit -m "feat: 添加更新图书请求 DTO"
```

---

## Task 5: 创建 DTO 类 - BatchDeleteRequest 和响应类

**Files:**

- Create: `backend/src/main/java/com/library/dto/BatchDeleteRequest.java`
- Create: `backend/src/main/java/com/library/dto/BatchDeleteResponse.java`
- Create: `backend/src/main/java/com/library/dto/ImportResponse.java`

- [ ] **Step 1: 创建 BatchDeleteRequest 类**

```java
package com.library.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchDeleteRequest {
    @NotEmpty(message = "图书ID列表不能为空")
    private List<Long> bookIds;
}
```

- [ ] **Step 2: 创建 BatchDeleteResponse 类**

```java
package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteResponse {
    private int successCount;
    private int failedCount;
    private List<BatchOperationFailure> failures = new ArrayList<>();
}
```

- [ ] **Step 3: 创建 ImportResponse 类**

```java
package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private int successCount;
    private int failedCount;
    private List<BatchOperationFailure> failures = new ArrayList<>();
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/library/dto/BatchDeleteRequest.java
git add backend/src/main/java/com/library/dto/BatchDeleteResponse.java
git add backend/src/main/java/com/library/dto/ImportResponse.java
git commit -m "feat: 添加批量操作请求和响应 DTO"
```

---

## Task 6: 创建文件解析工具类 - BookFileParser (Part 1)

**Files:**

- Create: `backend/src/main/java/com/library/util/BookFileParser.java`

- [ ] **Step 1: 创建 BookFileParser 类框架和 Excel 解析方法**

```java
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
```

- [ ] **Step 2: 提交第一部分**

```bash
git add backend/src/main/java/com/library/util/BookFileParser.java
git commit -m "feat: 添加 BookFileParser 工具类框架和 Excel 解析方法"
```

---

## Task 7: 完善 BookFileParser - 辅助方法

**Files:**

- Modify: `backend/src/main/java/com/library/util/BookFileParser.java`

- [ ] **Step 1: 添加 Excel 行解析和辅助方法**

在 `BookFileParser` 类中，替换 `// __CONTINUE_HERE__` 为以下代码：

```java
    private static Book parseExcelRow(Row row) {
        Book book = new Book();
        book.setTitle(getCellValueAsString(row.getCell(0)));
        book.setAuthor(getCellValueAsString(row.getCell(1)));
        book.setIsbn(getCellValueAsString(row.getCell(2)));
        book.setLocation(getCellValueAsString(row.getCell(3)));
        book.setCoverUrl(getCellValueAsString(row.getCell(4)));
        book.setStatus(getCellValueAsString(row.getCell(5)));
        book.setYear(getCellValueAsString(row.getCell(6)));
        book.setDescription(getCellValueAsString(row.getCell(7)));
        book.setLanguageCode(getCellValueAsString(row.getCell(8)));
        book.setAvailability(getCellValueAsString(row.getCell(9)));
        book.setCategory(getCellValueAsString(row.getCell(10)));
        
        String policyStr = getCellValueAsString(row.getCell(11));
        if (policyStr != null && !policyStr.isEmpty()) {
            try {
                book.setCirculationPolicy(Book.CirculationPolicy.valueOf(policyStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                book.setCirculationPolicy(Book.CirculationPolicy.AUTO);
            }
        } else {
            book.setCirculationPolicy(Book.CirculationPolicy.AUTO);
        }
        
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
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/util/BookFileParser.java
git commit -m "feat: 完善 BookFileParser Excel 行解析和辅助方法"
```

---

## Task 8: 完善 BookFileParser - CSV 解析和模板生成

**Files:**

- Modify: `backend/src/main/java/com/library/util/BookFileParser.java`

- [ ] **Step 1: 添加 CSV 解析方法**

在 `BookFileParser` 类末尾添加：

```java
    public static List<Book> parseCsv(MultipartFile file) throws IOException {
        List<Book> books = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            
            if (rows.size() < 2) {
                throw new IllegalArgumentException("CSV文件至少需要包含表头和一行数据");
            }
            
            String[] headers = rows.get(0);
            validateCsvHeaders(headers);
            
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (isEmptyCsvRow(row)) {
                    continue;
                }
                
                Book book = parseCsvRow(row);
                books.add(book);
            }
        } catch (CsvException e) {
            throw new IOException("CSV文件解析失败: " + e.getMessage(), e);
        }
        
        return books;
    }

    private static void validateCsvHeaders(String[] headers) {
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
        book.setCoverUrl(getValueOrNull(row, 4));
        book.setStatus(getValueOrNull(row, 5));
        book.setYear(getValueOrNull(row, 6));
        book.setDescription(getValueOrNull(row, 7));
        book.setLanguageCode(getValueOrNull(row, 8));
        book.setAvailability(getValueOrNull(row, 9));
        book.setCategory(getValueOrNull(row, 10));
        
        String policyStr = getValueOrNull(row, 11);
        if (policyStr != null && !policyStr.isEmpty()) {
            try {
                book.setCirculationPolicy(Book.CirculationPolicy.valueOf(policyStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                book.setCirculationPolicy(Book.CirculationPolicy.AUTO);
            }
        } else {
            book.setCirculationPolicy(Book.CirculationPolicy.AUTO);
        }
        
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
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getValueOrNull(String[] row, int index) {
        if (index >= row.length) {
            return null;
        }
        String value = row[index];
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/util/BookFileParser.java
git commit -m "feat: 添加 BookFileParser CSV 解析方法"
```

---

## Task 9: 完善 BookFileParser - 模板生成方法

**Files:**

- Modify: `backend/src/main/java/com/library/util/BookFileParser.java`

- [ ] **Step 1: 添加模板生成方法**

在 `BookFileParser` 类末尾添加：

```java
    public static byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("图书导入模板");
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "title", "author", "isbn", "location", "coverUrl", "status", 
                "year", "description", "languageCode", "availability", 
                "category", "circulationPolicy", "totalCopies"
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
            
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("Java编程思想");
            exampleRow.createCell(1).setCellValue("Bruce Eckel");
            exampleRow.createCell(2).setCellValue("978-0131872486");
            exampleRow.createCell(3).setCellValue("A区->1层->计算机类->001");
            exampleRow.createCell(4).setCellValue("https://example.com/cover.jpg");
            exampleRow.createCell(5).setCellValue("良好");
            exampleRow.createCell(6).setCellValue("2006");
            exampleRow.createCell(7).setCellValue("经典Java编程书籍");
            exampleRow.createCell(8).setCellValue("zh");
            exampleRow.createCell(9).setCellValue("可借阅");
            exampleRow.createCell(10).setCellValue("计算机");
            exampleRow.createCell(11).setCellValue("AUTO");
            exampleRow.createCell(12).setCellValue("5");
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/util/BookFileParser.java
git commit -m "feat: 添加 BookFileParser 模板生成方法"
```

---

## Task 10: 增强 BookService - 添加依赖注入

**Files:**

- Modify: `backend/src/main/java/com/library/service/BookService.java`

- [ ] **Step 1: 添加新的 Repository 依赖**

在 `BookService` 类中，找到现有的依赖注入部分，添加以下字段：

```java
private final ReservationRecordRepository reservationRecordRepository;
private final BookReviewRepository bookReviewRepository;
private final BookFavoriteRepository bookFavoriteRepository;
private final ReadingStatusRecordRepository readingStatusRecordRepository;
```

更新构造函数，添加这些参数：

```java
public BookService(
    BookRepository bookRepository,
    ReservationRecordRepository reservationRecordRepository,
    BookReviewRepository bookReviewRepository,
    BookFavoriteRepository bookFavoriteRepository,
    ReadingStatusRecordRepository readingStatusRecordRepository
) {
    this.bookRepository = bookRepository;
    this.reservationRecordRepository = reservationRecordRepository;
    this.bookReviewRepository = bookReviewRepository;
    this.bookFavoriteRepository = bookFavoriteRepository;
    this.readingStatusRecordRepository = readingStatusRecordRepository;
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/service/BookService.java
git commit -m "refactor: 添加 BookService 关联数据检查所需的依赖"
```

---

## Task 11: 增强 BookService - 添加验证和检查方法

**Files:**

- Modify: `backend/src/main/java/com/library/service/BookService.java`

- [ ] **Step 1: 添加 validateBook 方法**

在 `BookService` 类末尾添加：

```java
    public void validateBook(Book book) {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("作者不能为空");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        if (book.getLocation() == null || book.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("位置不能为空");
        }
        if (book.getTotalCopies() == null || book.getTotalCopies() < 1) {
            throw new IllegalArgumentException("总副本数必须至少为1");
        }
    }

    public void checkRelatedData(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getBorrowedCount() > 0) {
            throw new IllegalArgumentException("该图书仍有未归还记录，无法删除。");
        }

        long activeReservations = reservationRecordRepository.countByBookIdAndStatus(
            bookId, 
            com.library.model.ReservationRecord.ReservationStatus.WAITING
        ) + reservationRecordRepository.countByBookIdAndStatus(
            bookId, 
            com.library.model.ReservationRecord.ReservationStatus.AVAILABLE
        );
        if (activeReservations > 0) {
            throw new IllegalArgumentException("该图书有 " + activeReservations + " 条活跃预约记录，无法删除。");
        }

        long reviewCount = bookReviewRepository.countByBookId(bookId);
        if (reviewCount > 0) {
            throw new IllegalArgumentException("该图书有 " + reviewCount + " 条评论记录，无法删除。");
        }

        long favoriteCount = bookFavoriteRepository.countByUserId(bookId);
        if (favoriteCount > 0) {
            throw new IllegalArgumentException("该图书被 " + favoriteCount + " 位用户收藏，无法删除。");
        }

        long readingStatusCount = readingStatusRecordRepository.countByUserId(bookId);
        if (readingStatusCount > 0) {
            throw new IllegalArgumentException("该图书有 " + readingStatusCount + " 条阅读状态记录，无法删除。");
        }

        if (!book.getAvailableCopies().equals(book.getTotalCopies())) {
            log.warn("图书 {} 的可借数量({})与总数({})不一致", bookId, book.getAvailableCopies(), book.getTotalCopies());
            throw new IllegalArgumentException("图书库存数据异常，无法删除。");
        }
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/service/BookService.java
git commit -m "feat: 添加 BookService 验证和关联数据检查方法"
```

---

## Task 12: 增强 BookService - 添加库存调整方法

**Files:**

- Modify: `backend/src/main/java/com/library/service/BookService.java`

- [ ] **Step 1: 添加 adjustCopiesOnUpdate 方法**

在 `BookService` 类末尾添加：

```java
    public void adjustCopiesOnUpdate(Book existingBook, Book updatedBook) {
        if (updatedBook.getTotalCopies() == null) {
            return;
        }

        int newTotalCopies = updatedBook.getTotalCopies();
        int borrowedCount = existingBook.getBorrowedCount();

        if (newTotalCopies < borrowedCount) {
            throw new IllegalArgumentException(
                "总副本数(" + newTotalCopies + ")不能小于已借出数量(" + borrowedCount + ")"
            );
        }

        int newAvailableCopies = newTotalCopies - borrowedCount;
        updatedBook.setAvailableCopies(newAvailableCopies);
        updatedBook.setBorrowedCount(borrowedCount);

        log.info("图书 {} 库存调整: totalCopies={}, availableCopies={}, borrowedCount={}", 
            existingBook.getId(), newTotalCopies, newAvailableCopies, borrowedCount);
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/service/BookService.java
git commit -m "feat: 添加 BookService 库存调整方法"
```

---

## Task 13: 增强 BookService - 添加批量删除方法

**Files:**

- Modify: `backend/src/main/java/com/library/service/BookService.java`

- [ ] **Step 1: 添加 batchDeleteBooks 方法**

在 `BookService` 类末尾添加：

```java
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public com.library.dto.BatchDeleteResponse batchDeleteBooks(java.util.List<Long> bookIds) {
        com.library.dto.BatchDeleteResponse response = new com.library.dto.BatchDeleteResponse();
        int successCount = 0;
        int failedCount = 0;
        java.util.List<com.library.dto.BatchOperationFailure> failures = new java.util.ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                checkRelatedData(bookId);
                deleteBook(bookId);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failures.add(new com.library.dto.BatchOperationFailure(bookId, e.getMessage()));
                log.warn("批量删除图书 {} 失败: {}", bookId, e.getMessage());
            }
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setFailures(failures);

        log.info("批量删除图书完成: 成功 {}, 失败 {}", successCount, failedCount);
        return response;
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/service/BookService.java
git commit -m "feat: 添加 BookService 批量删除方法"
```

---

## Task 14: 增强 BookService - 添加批量导入方法

**Files:**

- Modify: `backend/src/main/java/com/library/service/BookService.java`

- [ ] **Step 1: 添加 batchCreateBooks 方法**

在 `BookService` 类末尾添加：

```java
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public com.library.dto.ImportResponse batchCreateBooks(java.util.List<com.library.model.Book> books) {
        com.library.dto.ImportResponse response = new com.library.dto.ImportResponse();
        int successCount = 0;
        int failedCount = 0;
        java.util.List<com.library.dto.BatchOperationFailure> failures = new java.util.ArrayList<>();
        java.util.Set<String> processedIsbns = new java.util.HashSet<>();

        for (int i = 0; i < books.size(); i++) {
            com.library.model.Book book = books.get(i);
            int rowNumber = i + 2;

            try {
                validateBook(book);

                if (processedIsbns.contains(book.getIsbn())) {
                    throw new IllegalArgumentException("ISBN '" + book.getIsbn() + "' 在导入文件中重复");
                }

                if (bookRepository.findByIsbn(book.getIsbn()).isPresent()) {
                    throw new IllegalArgumentException("ISBN '" + book.getIsbn() + "' 已存在");
                }

                book.setAvailableCopies(book.getTotalCopies());
                book.setBorrowedCount(0);

                createBook(book);
                processedIsbns.add(book.getIsbn());
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failures.add(new com.library.dto.BatchOperationFailure(rowNumber, e.getMessage()));
                log.warn("批量导入第 {} 行失败: {}", rowNumber, e.getMessage());
            }
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setFailures(failures);

        log.info("批量导入图书完成: 成功 {}, 失败 {}", successCount, failedCount);
        return response;
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/service/BookService.java
git commit -m "feat: 添加 BookService 批量导入方法"
```

---

## Task 15: 扩展 BookController - 添加创建图书接口

**Files:**

- Modify: `backend/src/main/java/com/library/controller/BookController.java`

- [ ] **Step 1: 添加创建图书接口**

在 `BookController` 类末尾添加：

```java
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@Valid @RequestBody com.library.dto.CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setLocation(request.getLocation());
        book.setCoverUrl(request.getCoverUrl());
        book.setStatus(request.getStatus());
        book.setYear(request.getYear());
        book.setDescription(request.getDescription());
        book.setLanguageCode(request.getLanguageCode());
        book.setAvailability(request.getAvailability());
        book.setCategory(request.getCategory());
        book.setCirculationPolicy(request.getCirculationPolicy());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setBorrowedCount(0);

        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            return ApiResponse.error("ISBN '" + request.getIsbn() + "' 已存在", 409);
        }

        bookService.validateBook(book);
        Book createdBook = bookService.createBook(book);
        return ApiResponse.ok(createdBook);
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/controller/BookController.java
git commit -m "feat: 添加创建图书管理接口"
```

---

## Task 16: 扩展 BookController - 添加更新和删除图书接口

**Files:**

- Modify: `backend/src/main/java/com/library/controller/BookController.java`

- [ ] **Step 1: 添加更新图书接口**

在 `BookController` 类末尾添加：

```java
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody com.library.dto.UpdateBookRequest request
    ) {
        Book existingBook = bookService.getBookById(id);
        if (existingBook == null) {
            return ApiResponse.notFound("图书不存在");
        }

        java.util.Optional<Book> duplicateIsbn = bookRepository.findByIsbn(request.getIsbn());
        if (duplicateIsbn.isPresent() && !duplicateIsbn.get().getId().equals(id)) {
            return ApiResponse.error("ISBN '" + request.getIsbn() + "' 已被其他图书使用", 409);
        }

        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setLocation(request.getLocation());
        existingBook.setCoverUrl(request.getCoverUrl());
        existingBook.setStatus(request.getStatus());
        existingBook.setYear(request.getYear());
        existingBook.setDescription(request.getDescription());
        existingBook.setLanguageCode(request.getLanguageCode());
        existingBook.setAvailability(request.getAvailability());
        existingBook.setCategory(request.getCategory());
        
        if (request.getCirculationPolicy() != null) {
            existingBook.setCirculationPolicy(request.getCirculationPolicy());
        }
        
        if (request.getTotalCopies() != null) {
            existingBook.setTotalCopies(request.getTotalCopies());
            bookService.adjustCopiesOnUpdate(existingBook, existingBook);
        }

        bookService.validateBook(existingBook);
        Book updatedBook = bookService.updateBook(existingBook);
        return ApiResponse.ok(updatedBook);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            return ApiResponse.notFound("图书不存在");
        }

        try {
            bookService.checkRelatedData(id);
            bookService.deleteBook(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 409);
        }
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/controller/BookController.java
git commit -m "feat: 添加更新和删除图书管理接口"
```

---

## Task 17: 扩展 BookController - 添加批量删除接口

**Files:**

- Modify: `backend/src/main/java/com/library/controller/BookController.java`

- [ ] **Step 1: 添加批量删除接口**

在 `BookController` 类末尾添加：

```java
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<com.library.dto.BatchDeleteResponse>> batchDeleteBooks(
        @Valid @RequestBody com.library.dto.BatchDeleteRequest request
    ) {
        com.library.dto.BatchDeleteResponse response = bookService.batchDeleteBooks(request.getBookIds());
        return ApiResponse.ok(response);
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/controller/BookController.java
git commit -m "feat: 添加批量删除图书接口"
```

---

## Task 18: 扩展 BookController - 添加批量导入和模板下载接口

**Files:**

- Modify: `backend/src/main/java/com/library/controller/BookController.java`

- [ ] **Step 1: 添加批量导入接口**

在 `BookController` 类末尾添加：

```java
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<com.library.dto.ImportResponse>> importBooks(
        @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ApiResponse.error("文件不能为空", 400);
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ApiResponse.error("文件名无效", 400);
        }

        try {
            java.util.List<Book> books;
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                books = com.library.util.BookFileParser.parseExcel(file);
            } else if (filename.endsWith(".csv")) {
                books = com.library.util.BookFileParser.parseCsv(file);
            } else {
                return ApiResponse.error("不支持的文件格式，仅支持 .xlsx, .xls, .csv", 400);
            }

            com.library.dto.ImportResponse response = bookService.batchCreateBooks(books);
            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 400);
        } catch (java.io.IOException e) {
            return ApiResponse.error("文件解析失败: " + e.getMessage(), 400);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/import/template")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTemplate() {
        try {
            byte[] templateBytes = com.library.util.BookFileParser.generateTemplate();
            org.springframework.core.io.ByteArrayResource resource = 
                new org.springframework.core.io.ByteArrayResource(templateBytes);

            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=book_import_template.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(templateBytes.length)
                .body(resource);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/library/controller/BookController.java
git commit -m "feat: 添加批量导入和模板下载接口"
```

---

## Task 19: 编写 BookService 单元测试

**Files:**

- Create: `backend/src/test/java/com/library/service/BookServiceTest.java`

- [ ] **Step 1: 创建 BookServiceTest 测试类框架**

```java
package com.library.service;

import com.library.dto.BatchDeleteResponse;
import com.library.dto.ImportResponse;
import com.library.model.Book;
import com.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReservationRecordRepository reservationRecordRepository;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookFavoriteRepository bookFavoriteRepository;

    @Mock
    private ReadingStatusRecordRepository readingStatusRecordRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("978-1234567890");
        testBook.setLocation("A1-001");
        testBook.setTotalCopies(5);
        testBook.setAvailableCopies(5);
        testBook.setBorrowedCount(0);
        testBook.setCirculationPolicy(Book.CirculationPolicy.AUTO);
    }

    // __CONTINUE_HERE__
}
```

- [ ] **Step 2: 提交测试框架**

```bash
git add backend/src/test/java/com/library/service/BookServiceTest.java
git commit -m "test: 添加 BookService 单元测试框架"
```

---

## Task 20: 完善 BookServiceTest - 添加验证测试

**Files:**

- Modify: `backend/src/test/java/com/library/service/BookServiceTest.java`

- [ ] **Step 1: 添加 validateBook 测试方法**

在 `BookServiceTest` 类中，替换 `// __CONTINUE_HERE__` 为：

```java
    @Test
    void testValidateBook_Success() {
        assertDoesNotThrow(() -> bookService.validateBook(testBook));
    }

    @Test
    void testValidateBook_MissingTitle() {
        testBook.setTitle(null);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("标题不能为空", exception.getMessage());
    }

    @Test
    void testValidateBook_MissingAuthor() {
        testBook.setAuthor("");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("作者不能为空", exception.getMessage());
    }

    @Test
    void testValidateBook_MissingIsbn() {
        testBook.setIsbn(null);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("ISBN不能为空", exception.getMessage());
    }

    @Test
    void testValidateBook_InvalidTotalCopies() {
        testBook.setTotalCopies(0);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.validateBook(testBook)
        );
        assertEquals("总副本数必须至少为1", exception.getMessage());
    }

    @Test
    void testAdjustCopiesOnUpdate_Success() {
        Book existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTotalCopies(5);
        existingBook.setAvailableCopies(3);
        existingBook.setBorrowedCount(2);

        Book updatedBook = new Book();
        updatedBook.setTotalCopies(10);

        bookService.adjustCopiesOnUpdate(existingBook, updatedBook);

        assertEquals(10, updatedBook.getTotalCopies());
        assertEquals(8, updatedBook.getAvailableCopies());
        assertEquals(2, updatedBook.getBorrowedCount());
    }

    @Test
    void testAdjustCopiesOnUpdate_TotalCopiesLessThanBorrowed() {
        Book existingBook = new Book();
        existingBook.setBorrowedCount(5);

        Book updatedBook = new Book();
        updatedBook.setTotalCopies(3);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bookService.adjustCopiesOnUpdate(existingBook, updatedBook)
        );
        assertTrue(exception.getMessage().contains("不能小于已借出数量"));
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run: `mvn test -Dtest=BookServiceTest`
Expected: All tests pass

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/library/service/BookServiceTest.java
git commit -m "test: 添加 BookService 验证和库存调整测试"
```

---

## Task 21: 编写 BookController 集成测试

**Files:**

- Create: `backend/src/test/java/com/library/controller/BookControllerAdminTest.java`

- [ ] **Step 1: 创建集成测试类**

```java
package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.CreateBookRequest;
import com.library.dto.UpdateBookRequest;
import com.library.dto.BatchDeleteRequest;
import com.library.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookControllerAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_Success() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("New Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("978-9999999999");
        request.setLocation("A1-999");
        request.setTotalCopies(3);

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("New Test Book"))
            .andExpect(jsonPath("$.data.isbn").value("978-9999999999"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateBook_Forbidden() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Test");
        request.setAuthor("Author");
        request.setIsbn("123");
        request.setLocation("A1");

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_ValidationError() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("");
        request.setAuthor("Author");
        request.setIsbn("123");
        request.setLocation("A1");

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `mvn test -Dtest=BookControllerAdminTest`
Expected: All tests pass

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/library/controller/BookControllerAdminTest.java
git commit -m "test: 添加 BookController 管理接口集成测试"
```

---

## Task 22: 手动测试和验证

**Files:**

- N/A (手动测试)

- [ ] **Step 1: 启动应用**

Run: `mvn spring-boot:run`
Expected: Application starts successfully

- [ ] **Step 2: 测试创建图书 API**

使用 Postman 或 curl 测试：

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "title": "手动测试图书",
    "author": "测试作者",
    "isbn": "978-1111111111",
    "location": "A1-TEST",
    "totalCopies": 3
  }'
```

Expected: 返回 200 OK，包含创建的图书信息

- [ ] **Step 3: 测试更新图书 API**

```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "title": "更新后的标题",
    "author": "测试作者",
    "isbn": "978-1111111111",
    "location": "A1-TEST",
    "totalCopies": 5
  }'
```

Expected: 返回 200 OK，availableCopies 自动调整

- [ ] **Step 4: 测试删除图书 API**

```bash
curl -X DELETE http://localhost:8080/api/books/999 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

Expected: 返回 200 OK 或 409 Conflict（如有关联数据）

- [ ] **Step 5: 测试批量删除 API**

```bash
curl -X DELETE http://localhost:8080/api/books/batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "bookIds": [100, 101, 102]
  }'
```

Expected: 返回批量操作结果，包含成功和失败统计

- [ ] **Step 6: 测试下载导入模板**

```bash
curl -X GET http://localhost:8080/api/books/import/template \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -o template.xlsx
```

Expected: 下载 Excel 模板文件

- [ ] **Step 7: 测试批量导入 API**

准备测试 Excel 文件，然后：

```bash
curl -X POST http://localhost:8080/api/books/import \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -F "file=@test_books.xlsx"
```

Expected: 返回导入结果，包含成功和失败统计

- [ ] **Step 8: 验证 Elasticsearch 同步**

检查 Elasticsearch 中是否有新创建的图书数据：

```bash
curl -X GET http://localhost:9200/books/_search?q=title:手动测试图书
```

Expected: 返回对应的图书文档

---

## Task 23: 更新 API 文档

**Files:**

- Create or Modify: `docs/api/book-management-api.md` (如果项目有 API 文档)

- [ ] **Step 1: 创建 API 文档**

创建文档记录新增的管理接口：

```markdown
# 图书管理 API

## 权限要求
所有接口仅限 ADMIN 角色访问。

## 接口列表

### 1. 创建图书
POST /api/books

### 2. 更新图书
PUT /api/books/{id}

### 3. 删除图书
DELETE /api/books/{id}

### 4. 批量删除
DELETE /api/books/batch

### 5. 批量导入
POST /api/books/import

### 6. 下载导入模板
GET /api/books/import/template
```

- [ ] **Step 2: 提交文档**

```bash
git add docs/api/book-management-api.md
git commit -m "docs: 添加图书管理 API 文档"
```

---

## Task 24: 最终验证和清理

**Files:**

- N/A

- [ ] **Step 1: 运行完整测试套件**

Run: `mvn clean test`
Expected: All tests pass

- [ ] **Step 2: 检查代码覆盖率**

Run: `mvn test jacoco:report`
Expected: 覆盖率报告生成

- [ ] **Step 3: 代码格式检查**

Run: `mvn spotless:check` (如果项目配置了 Spotless)
Expected: No formatting issues

- [ ] **Step 4: 构建项目**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 最终提交**

```bash
git add .
git commit -m "feat: 完成图书管理 API 实现

- 添加创建、更新、删除图书接口
- 添加批量删除和批量导入功能
- 支持 CSV 和 Excel 文件导入
- 添加完整的数据验证和关联检查
- 添加单元测试和集成测试
- 更新 API 文档"
```

---

## 实施总结

**已完成功能：**

1. ✅ 单个图书的创建、更新、删除接口
2. ✅ 批量删除图书接口
3. ✅ 批量导入图书接口（支持 CSV 和 Excel）
4. ✅ 下载导入模板接口
5. ✅ 完整的数据验证和关联数据检查
6. ✅ 库存自动调整逻辑
7. ✅ 单元测试和集成测试
8. ✅ API 文档

**技术要点：**

- 使用 Spring Security `@PreAuthorize` 进行权限控制
- 使用 Bean Validation 进行数据验证
- 使用 `@Transactional(propagation = REQUIRES_NEW)` 处理批量操作
- 依赖 BookSyncListener 自动同步 Elasticsearch
- 使用 Apache POI 解析 Excel，OpenCSV 解析 CSV

**测试覆盖：**

- BookService 单元测试：验证、库存调整
- BookController 集成测试：权限控制、数据验证
- 手动测试：完整的 API 功能验证

**下一步建议：**

1. 根据实际使用情况优化批量操作性能
2. 添加图书导出功能（导出为 CSV/Excel）
3. 添加图书审计日志
4. 考虑添加图书批量修改功能

