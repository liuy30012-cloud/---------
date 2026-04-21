package com.library.listener;

import com.library.model.Book;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class BookSyncListener {

    private static ElasticsearchSyncService syncService;
    private static final AtomicBoolean SYNC_ENABLED = new AtomicBoolean(true);

    @Autowired(required = false)
    public void setSyncService(ElasticsearchSyncService syncService) {
        BookSyncListener.syncService = syncService;
    }

    public static boolean isSyncEnabled() {
        return SYNC_ENABLED.get();
    }

    public static void setSyncEnabled(boolean enabled) {
        SYNC_ENABLED.set(enabled);
    }

    @PostPersist
    public void onBookCreated(Book book) {
        log.debug("图书创建事件触发: {}", book.getId());
        if (syncService != null && SYNC_ENABLED.get()) {
            syncService.indexBook(book);
        }
    }

    @PostUpdate
    public void onBookUpdated(Book book) {
        log.debug("图书更新事件触发: {}", book.getId());
        if (syncService != null && SYNC_ENABLED.get()) {
            syncService.updateBook(book);
        }
    }

    @PostRemove
    public void onBookDeleted(Book book) {
        log.debug("图书删除事件触发: {}", book.getId());
        if (syncService != null && SYNC_ENABLED.get()) {
            syncService.deleteBook(book.getId());
        }
    }
}
