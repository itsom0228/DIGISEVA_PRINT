package com.printwithai.printer;

import com.printwithai.entity.KioskLog;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PrintJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MockPrinterService implements PrinterService {

    private static final Logger log = LoggerFactory.getLogger(MockPrinterService.class);

    private final PrintJobRepository printJobRepository;
    private final KioskLogRepository kioskLogRepository;

    public MockPrinterService(PrintJobRepository printJobRepository, KioskLogRepository kioskLogRepository) {
        this.printJobRepository = printJobRepository;
        this.kioskLogRepository = kioskLogRepository;
    }

    @Override
    public void submitJob(PrintJob job) {
        log.info("[SIMULATED PRINTING] Received print job {}. No physical printer triggered in demo mode.", job.getPublicReference());

        // Step 1: Set to QUEUED
        job.setJobStatus(PrintJobStatus.QUEUED);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "JOB_QUEUED",
                String.format("Order %s queued for printing (%d pages, %d copies, %s). SIMULATED PRINTING.",
                        job.getPublicReference(), job.getSelectedPageCount(), job.getCopies(), job.getPrintType().getDisplayName())
        ));

        // Step 2 & 3: Simulate realistic printing progress asynchronously
        simulateAsyncPrinting(job.getId());
    }

    private void simulateAsyncPrinting(Long jobId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Wait 2 seconds in QUEUE
                TimeUnit.SECONDS.sleep(2);

                printJobRepository.findById(jobId).ifPresent(job -> {
                    if (job.getJobStatus() == PrintJobStatus.QUEUED) {
                        job.setJobStatus(PrintJobStatus.PRINTING);
                        printJobRepository.save(job);
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "JOB_PRINTING",
                                "Printer hardware engaged. Printing pages in progress..."
                        ));
                    }
                });

                // Simulate printing duration based on pages (3 seconds)
                TimeUnit.SECONDS.sleep(3);

                printJobRepository.findById(jobId).ifPresent(job -> {
                    if (job.getJobStatus() == PrintJobStatus.PRINTING) {
                        job.setJobStatus(PrintJobStatus.COMPLETED);
                        job.setCompletedAt(LocalDateTime.now());
                        printJobRepository.save(job);
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "JOB_COMPLETED",
                                "Print job successfully finished. Document ready in pickup tray. (SIMULATION)"
                        ));
                        log.info("[SIMULATED PRINTING] Job {} completed successfully.", job.getPublicReference());
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Mock printing simulation interrupted for job ID: {}", jobId);
            } catch (Exception e) {
                log.error("Error during mock printing simulation for job {}: {}", jobId, e.getMessage());
            }
        });
    }

    @Override
    public void retryJob(PrintJob job) {
        log.info("[SIMULATED PRINTING] Retrying job {}", job.getPublicReference());
        job.setJobStatus(PrintJobStatus.QUEUED);
        job.setFailureReason(null);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "JOB_RETRY",
                "Administrative retry triggered for order " + job.getPublicReference()
        ));

        simulateAsyncPrinting(job.getId());
    }

    @Override
    public void cancelJob(PrintJob job) {
        log.info("[SIMULATED PRINTING] Cancelling job {}", job.getPublicReference());
        job.setJobStatus(PrintJobStatus.CANCELLED);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "JOB_CANCELLED",
                "Order cancelled by administrator/system."
        ));
    }

    @Override
    public String getPrinterStatus() {
        return "ONLINE (Virtual Demonstration Thermal/Laser Adapter Ready)";
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
