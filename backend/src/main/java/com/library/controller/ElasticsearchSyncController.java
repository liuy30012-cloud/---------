package com.library.controller;

import com.library.service.elasticsearch.ElasticsearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/elasticsearch")
@RequiredArgsConstructor
public class ElasticsearchSyncController {

    private final ElasticsearchSyncService syncService;

    @PostMapping("/sync-all")
    public ResponseEntity<String> syncAllBooks() {
        try {
            syncService.syncAllBooks();
            return ResponseEntity.ok("全量同步成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("全量同步失败: " + e.getMessage());
        }
    }
}
