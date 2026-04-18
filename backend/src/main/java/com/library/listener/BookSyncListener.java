package com.library.listener;

import com.library.model.Book;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookSyncListener {

    private static ElasticsearchSyncService syncService;

    @Autowired(required = false)
    public void setSyncService(ElasticsearchSyncService syncService) {
        BookSyncListener.syncService = syncService;
    }

    @PostPersist
    public void onBookCreated(Book book) {
        log.debug("图书创建事件触发: {}", book.getId());
        if (syncService != null) {
            syncService.indexBook(book);
        }
    }

    @PostUpdate
    public void onBookUpdated(Book book) {
        log.debug("图书更新事件触发: {}", book.getId());
        if (syncService != null) {
            syncService.updateBook(book);
        }
    }

    @PostRemove
    public void onBookDeleted(Book book) {
        log.debug("图书删除事件触发: {}", book.getId());
        if (syncService != null) {
            syncService.deleteBook(book.getId());
        }
    }
}
