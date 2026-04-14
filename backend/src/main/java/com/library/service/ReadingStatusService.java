package com.library.service;

import com.library.dto.ReadingStatusResponse;
import com.library.exception.ResourceNotFoundException;
import com.library.model.ReadingStatus;
import com.library.model.ReadingStatusRecord;
import com.library.repository.BookRepository;
import com.library.repository.ReadingStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingStatusService {

    private final ReadingStatusRecordRepository readingStatusRecordRepository;
    private final BookRepository bookRepository;

    @Transactional
    public ReadingStatusResponse upsertReadingStatus(Long userId, Long bookId, ReadingStatus status, String notes) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("图书不存在，ID: " + bookId);
        }

        ReadingStatusRecord record = readingStatusRecordRepository
                .findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> {
                    ReadingStatusRecord newRecord = new ReadingStatusRecord();
                    newRecord.setUserId(userId);
                    newRecord.setBookId(bookId);
                    return newRecord;
                });

        ReadingStatus oldStatus = record.getStatus();
        record.setStatus(status);
        record.setNotes(notes);

        if (status == ReadingStatus.READING && record.getStartedAt() == null) {
            record.setStartedAt(LocalDateTime.now());
        }
        if (status == ReadingStatus.READ) {
            if (record.getStartedAt() == null) {
                record.setStartedAt(LocalDateTime.now());
            }
            record.setFinishedAt(LocalDateTime.now());
        }

        if (oldStatus == ReadingStatus.READ && status != ReadingStatus.READ) {
            record.setFinishedAt(null);
        }

        ReadingStatusRecord saved = readingStatusRecordRepository.save(record);
        return convertToResponse(saved);
    }

    @Transactional
    public void removeReadingStatus(Long userId, Long bookId) {
        if (!readingStatusRecordRepository.findByUserIdAndBookId(userId, bookId).isPresent()) {
            throw new ResourceNotFoundException("该图书没有阅读状态记录");
        }
        readingStatusRecordRepository.deleteByUserIdAndBookId(userId, bookId);
    }

    public ReadingStatusResponse getReadingStatus(Long userId, Long bookId) {
        return readingStatusRecordRepository.findByUserIdAndBookId(userId, bookId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    public List<ReadingStatusResponse> getUserReadingStatuses(Long userId, ReadingStatus status) {
        List<ReadingStatusRecord> records;
        if (status != null) {
            records = readingStatusRecordRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, status);
        } else {
            records = readingStatusRecordRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getStatusCounts(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        for (ReadingStatus status : ReadingStatus.values()) {
            counts.put(status.name(), readingStatusRecordRepository.countByUserIdAndStatus(userId, status));
        }
        counts.put("TOTAL", readingStatusRecordRepository.countByUserId(userId));
        return counts;
    }

    private ReadingStatusResponse convertToResponse(ReadingStatusRecord record) {
        ReadingStatusResponse response = new ReadingStatusResponse();
        response.setId(record.getId());
        response.setBookId(record.getBookId());
        response.setStatus(record.getStatus().name());
        response.setNotes(record.getNotes());
        response.setStartedAt(record.getStartedAt());
        response.setFinishedAt(record.getFinishedAt());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        bookRepository.findById(record.getBookId()).ifPresent(book -> {
            response.setBookTitle(book.getTitle());
            response.setAuthor(book.getAuthor());
            response.setCoverUrl(book.getCoverUrl());
            response.setCategory(book.getCategory());
        });

        return response;
    }
}
