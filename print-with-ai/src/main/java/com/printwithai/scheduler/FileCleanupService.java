package com.printwithai.scheduler;

import com.printwithai.entity.PrintJob;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final PrintJobRepository printJobRepository;
    private final StorageService storageService;
    private final int retentionMinutes;

    public FileCleanupService(
            PrintJobRepository printJobRepository,
            StorageService storageService,
            @Value("${app.storage.retention-minutes:60}") int retentionMinutes) {
        this.printJobRepository = printJobRepository;
        this.storageService = storageService;
        this.retentionMinutes = retentionMinutes;
    }

    /**
     * Runs periodically every 15 minutes to purge expired uploaded documents.
     */
    @Scheduled(fixedRate = 900000) // 15 mins
    @Transactional
    public int cleanupExpiredDocuments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(retentionMinutes);
        List<PrintJob> expiredJobs = printJobRepository.findByCreatedAtBeforeAndFileDeletedFalse(cutoff);

        int cleanedCount = 0;
        for (PrintJob job : expiredJobs) {
            try {
                boolean deleted = storageService.deleteFile(job.getStoredFilename());
                if (deleted) {
                    job.setFileDeleted(true);
                    job.setFileDeletedAt(LocalDateTime.now());
                    printJobRepository.save(job);
                    cleanedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to delete expired file for order {}: {}", job.getPublicReference(), e.getMessage());
            }
        }

        if (cleanedCount > 0) {
            log.info("[FILE PRIVACY CLEANUP] Successfully purged {} expired document files (Retention: {} mins)",
                    cleanedCount, retentionMinutes);
        }

        return cleanedCount;
    }

    public int getRetentionMinutes() {
        return retentionMinutes;
    }
}
