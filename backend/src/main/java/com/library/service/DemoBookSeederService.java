package com.library.service;

import com.library.listener.BookSyncListener;
import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.service.elasticsearch.ElasticsearchSyncService;
import com.library.util.BookImportSupport;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoBookSeederService {

    private static final int BATCH_SIZE = 1_000;

    private final BookRepository bookRepository;
    private final ObjectProvider<ElasticsearchSyncService> elasticsearchSyncServiceProvider;

    @PersistenceContext
    private EntityManager entityManager;

    public SeedReport seedFromResource(
        Resource chineseShowcaseResource,
        Resource resource,
        int targetCount,
        boolean disableEsListenerDuringSeed
    )
        throws IOException, CsvException {
        boolean hasShowcaseResource = chineseShowcaseResource != null && chineseShowcaseResource.exists();
        boolean hasBulkResource = resource != null && resource.exists();
        if (!hasShowcaseResource && !hasBulkResource) {
            throw new IllegalStateException("No demo book resources are available.");
        }

        boolean previousSyncState = BookSyncListener.isSyncEnabled();
        if (disableEsListenerDuringSeed) {
            BookSyncListener.setSyncEnabled(false);
        }

        try {
            SeedAccumulator accumulator = new SeedAccumulator(loadKnownIsbns());

            if (chineseShowcaseResource != null && chineseShowcaseResource.exists() && targetCount > 0) {
                importBooks(chineseShowcaseResource, targetCount, accumulator, false);
            }

            int remaining = Math.max(targetCount - accumulator.importedCount, 0);
            if (remaining > 0 && hasBulkResource) {
                importBooks(resource, remaining, accumulator, true);
            }

            SeedReport report = accumulator.toReport();
            ElasticsearchSyncService elasticsearchSyncService = elasticsearchSyncServiceProvider.getIfAvailable();
            if (report.getImportedCount() > 0 && elasticsearchSyncService != null) {
                elasticsearchSyncService.syncAllBooks();
            }
            return report;
        } finally {
            if (disableEsListenerDuringSeed) {
                BookSyncListener.setSyncEnabled(previousSyncState);
            }
        }
    }

    private Set<String> loadKnownIsbns() {
        Set<String> knownIsbns = new HashSet<>();
        for (String isbn : bookRepository.findAllIsbns()) {
            String normalized = BookImportSupport.normalizeIsbn(isbn);
            if (!normalized.isEmpty()) {
                knownIsbns.add(normalized);
            }
        }
        return knownIsbns;
    }

    private void importBooks(Resource resource, int targetCount, SeedAccumulator accumulator, boolean warnIfShortImport)
        throws IOException, CsvException {
        if (resource == null || !resource.exists() || targetCount <= 0) {
            return;
        }

        accumulator.currentRunBaseline = accumulator.importedCount;
        List<Book> batch = new ArrayList<>(BATCH_SIZE);

        try (InputStream sourceStream = openPossiblyCompressed(resource);
             InputStreamReader reader = new InputStreamReader(sourceStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                throw new IllegalArgumentException("Demo books CSV is empty.");
            }

            Map<String, Integer> headerIndexes =
                BookImportSupport.buildHeaderIndexes(Arrays.asList(headerRow));
            BookImportSupport.validateHeaders(headerIndexes.keySet());

            String[] row;
            while ((row = csvReader.readNext()) != null && accumulator.importedCountForCurrentRun() < targetCount) {
                accumulator.processedRows++;
                if (isEmptyRow(row)) {
                    continue;
                }

                Book book = BookImportSupport.parseBook(BookImportSupport.toRowMap(row, headerIndexes));
                String normalizedIsbn = BookImportSupport.normalizeIsbn(book.getIsbn());
                if (normalizedIsbn.isEmpty() || !accumulator.knownIsbns.add(normalizedIsbn)) {
                    accumulator.skippedDuplicates++;
                    continue;
                }

                applyInventoryPattern(book, accumulator.importedCount);
                batch.add(book);
                accumulator.importedCount++;

                if (batch.size() >= BATCH_SIZE) {
                    persistBatch(batch);
                    log.info("Seeded {} demo books so far.", accumulator.importedCount);
                }
            }
        }

        persistBatch(batch);

        if (warnIfShortImport && accumulator.importedCountForCurrentRun() < targetCount) {
            log.warn(
                "Stopped seeding resource {} after importing {} records; target for this run was {}.",
                resource,
                accumulator.importedCountForCurrentRun(),
                targetCount
            );
        }
    }

    @Transactional
    protected void persistBatch(List<Book> batch) {
        if (batch.isEmpty()) {
            return;
        }
        bookRepository.saveAll(batch);
        bookRepository.flush();
        entityManager.clear();
        batch.clear();
    }

    private void applyInventoryPattern(Book book, int importedIndex) {
        int pattern = importedIndex % 20;
        int totalCopies = book.getTotalCopies() == null || book.getTotalCopies() < 1 ? 1 : book.getTotalCopies();

        if (pattern == 19) {
            book.setCirculationPolicy(Book.CirculationPolicy.REFERENCE_ONLY);
            book.setTotalCopies(1);
            book.setAvailableCopies(1);
            book.setBorrowedCount(0);
            book.setStatus("REFERENCE_ONLY");
            book.setAvailability("Reading Room Only");
            return;
        }

        if (pattern >= 16) {
            int adjustedTotalCopies = Math.max(totalCopies, 2);
            int borrowedCount = pattern == 18 ? adjustedTotalCopies : Math.max(1, adjustedTotalCopies / 2);
            int availableCopies = Math.max(adjustedTotalCopies - borrowedCount, 0);

            book.setCirculationPolicy(book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY
                ? Book.CirculationPolicy.AUTO
                : book.getCirculationPolicy());
            book.setTotalCopies(adjustedTotalCopies);
            book.setAvailableCopies(availableCopies);
            book.setBorrowedCount(borrowedCount);
            book.setStatus(availableCopies == 0 ? "CHECKED_OUT" : "PARTIALLY_AVAILABLE");
            book.setAvailability(availableCopies == 0 ? "Checked Out" : "Limited Availability");
            return;
        }

        book.setCirculationPolicy(book.getCirculationPolicy() == null
            ? Book.CirculationPolicy.AUTO
            : book.getCirculationPolicy());
        book.setAvailableCopies(totalCopies);
        book.setBorrowedCount(0);
        book.setStatus("AVAILABLE");
        book.setAvailability("Available");
    }

    private boolean isEmptyRow(String[] row) {
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

    private InputStream openPossiblyCompressed(Resource resource) throws IOException {
        PushbackInputStream inputStream = new PushbackInputStream(resource.getInputStream(), 2);
        byte[] signature = new byte[2];
        int bytesRead = inputStream.read(signature);
        if (bytesRead > 0) {
            inputStream.unread(signature, 0, bytesRead);
        }

        boolean isGzip = bytesRead == 2
            && signature[0] == (byte) GZIPInputStream.GZIP_MAGIC
            && signature[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);

        return isGzip ? new GZIPInputStream(inputStream) : inputStream;
    }

    @lombok.Value
    public static class SeedReport {
        int processedRows;
        int importedCount;
        int skippedDuplicates;
    }

    private static final class SeedAccumulator {
        private final Set<String> knownIsbns;
        private int processedRows;
        private int importedCount;
        private int skippedDuplicates;
        private int currentRunBaseline;

        private SeedAccumulator(Set<String> knownIsbns) {
            this.knownIsbns = knownIsbns;
            this.currentRunBaseline = 0;
        }

        private int importedCountForCurrentRun() {
            return importedCount - currentRunBaseline;
        }

        private SeedReport toReport() {
            return new SeedReport(processedRows, importedCount, skippedDuplicates);
        }
    }
}
