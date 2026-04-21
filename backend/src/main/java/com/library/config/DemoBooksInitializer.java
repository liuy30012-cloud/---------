package com.library.config;

import com.library.repository.BookRepository;
import com.library.service.DemoBookSeederService;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class DemoBooksInitializer implements ApplicationRunner {

    private final DemoBooksProperties demoBooksProperties;
    private final BookRepository bookRepository;
    private final DemoBookSeederService demoBookSeederService;

    @Override
    public void run(ApplicationArguments args) throws IOException, CsvException {
        if (!demoBooksProperties.isEnabled()) {
            return;
        }

        long existingBookCount = bookRepository.count();
        if (demoBooksProperties.isSeedIfEmptyOnly() && existingBookCount > 0) {
            log.info("Skipping demo book seeding because the catalog already contains {} books.", existingBookCount);
            return;
        }

        boolean hasShowcaseResource = demoBooksProperties.getChineseShowcaseResource() != null
            && demoBooksProperties.getChineseShowcaseResource().exists();
        boolean hasBulkResource = demoBooksProperties.getResource() != null
            && demoBooksProperties.getResource().exists();

        if (!hasShowcaseResource && !hasBulkResource) {
            log.warn(
                "Skipping demo book seeding because both showcase resource {} and bulk resource {} are missing.",
                demoBooksProperties.getChineseShowcaseResource(),
                demoBooksProperties.getResource()
            );
            return;
        }

        DemoBookSeederService.SeedReport report = demoBookSeederService.seedFromResource(
            demoBooksProperties.getChineseShowcaseResource(),
            demoBooksProperties.getResource(),
            demoBooksProperties.getTargetCount(),
            demoBooksProperties.isDisableEsListenerDuringSeed()
        );

        log.info(
            "Demo book seeding finished. processedRows={}, importedCount={}, skippedDuplicates={}",
            report.getProcessedRows(),
            report.getImportedCount(),
            report.getSkippedDuplicates()
        );
    }
}
