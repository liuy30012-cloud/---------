package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.config.TestElasticsearchConfig;
import com.library.dto.CreateBookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestElasticsearchConfig.class)
@EnableAutoConfiguration(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class
})
class BookControllerAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testCreateBook_Success() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("New Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("978-9999999999");
        request.setLocation("A1-999");
        request.setTotalCopies(3);

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("New Test Book"))
            .andExpect(jsonPath("$.data.isbn").value("978-9999999999"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testCreateBook_Forbidden() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Test");
        request.setAuthor("Author");
        request.setIsbn("123");
        request.setLocation("A1");

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testCreateBook_ValidationError() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("");
        request.setAuthor("Author");
        request.setIsbn("123");
        request.setLocation("A1");

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testImportBooks_Success() throws Exception {
        String csv = String.join("\n",
            "title,author,isbn,location,coverUrl,status,year,description,languageCode,availability,category,circulationPolicy,totalCopies",
            "Import Test Book,Import Author,9781111111111,A区>1层>测试>001,https://covers.openlibrary.org/b/olid/OL1M-M.jpg?default=false,AVAILABLE,2024,Imported for integration testing,en,Available,测试,MANUAL,3"
        );

        MockMultipartFile file = new MockMultipartFile("file", "books.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/books/import")
                .file(file)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0));
    }
}
