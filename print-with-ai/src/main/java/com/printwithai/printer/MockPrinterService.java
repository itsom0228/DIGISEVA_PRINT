package com.printwithai.printer;

import com.printwithai.entity.KioskLog;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.storage.StorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MockPrinterService implements PrinterService {

    private static final Logger log = LoggerFactory.getLogger(MockPrinterService.class);

    private final PrintJobRepository printJobRepository;
    private final KioskLogRepository kioskLogRepository;
    private final StorageService storageService;

    @org.springframework.beans.factory.annotation.Value("${app.printer.name:}")
    private String configuredPrinterName;

    public MockPrinterService(PrintJobRepository printJobRepository,
                              KioskLogRepository kioskLogRepository,
                              StorageService storageService) {
        this.printJobRepository = printJobRepository;
        this.kioskLogRepository = kioskLogRepository;
        this.storageService = storageService;
    }

    @Override
    public void submitJob(PrintJob job) {
        log.info("[PRINT DISPATCH] Received print job {}. Preparing spooling...", job.getPublicReference());

        // Step 1: Set to QUEUED
        job.setJobStatus(PrintJobStatus.QUEUED);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "JOB_QUEUED",
                String.format("Order %s queued for printing (%d pages, %d copies, %s).",
                        job.getPublicReference(), job.getSelectedPageCount(), job.getCopies(), job.getPrintType().getDisplayName())
        ));

        // Step 2 & 3: Trigger printing progress asynchronously
        simulateAsyncPrinting(job.getId());
    }

    private void simulateAsyncPrinting(Long jobId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Wait 1 second in QUEUE
                TimeUnit.SECONDS.sleep(1);

                final PrintJob[] activeJob = new PrintJob[1];

                printJobRepository.findById(jobId).ifPresent(job -> {
                    if (job.getJobStatus() == PrintJobStatus.QUEUED) {
                        job.setJobStatus(PrintJobStatus.PRINTING);
                        printJobRepository.save(job);
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "JOB_PRINTING",
                                "Printing started. Dispatching to hardware printer..."
                        ));
                        activeJob[0] = job;
                    }
                });

                // Dispatch to physical connected printer
                if (activeJob[0] != null) {
                    dispatchToPhysicalPrinter(activeJob[0]);
                }

                // Wait 2 seconds for spool completion
                TimeUnit.SECONDS.sleep(2);

                printJobRepository.findById(jobId).ifPresent(job -> {
                    if (job.getJobStatus() == PrintJobStatus.PRINTING) {
                        job.setJobStatus(PrintJobStatus.COMPLETED);
                        job.setCompletedAt(LocalDateTime.now());
                        printJobRepository.save(job);
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "JOB_COMPLETED",
                                "Print job successfully finished. Document ready in pickup tray."
                        ));
                        log.info("[PRINT DISPATCH] Job {} completed successfully.", job.getPublicReference());
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Printing execution interrupted for job ID: {}", jobId);
            } catch (Exception e) {
                log.error("Error during printing execution for job {}: {}", jobId, e.getMessage());
            }
        });
    }

    /**
     * Sends the file directly to the OS default/connected physical printer using Java Print Service & PDFBox.
     */
    private void dispatchToPhysicalPrinter(PrintJob job) {
        Path filePath = null;
        File file = null;
        try {
            filePath = storageService.load(job.getStoredFilename());
            file = filePath.toFile();
            if (!file.exists()) {
                log.warn("[HARDWARE PRINT] File {} does not exist on disk for physical printing.", job.getStoredFilename());
                return;
            }

            PrintService targetPrinter = null;
            PrintService[] availableServices = null;
            try {
                availableServices = PrintServiceLookup.lookupPrintServices(null, null);
            } catch (Throwable t) {
                log.warn("[HARDWARE PRINT] Error looking up print services: {}", t.getMessage());
            }

            if (availableServices != null) {
                log.info("[AUTOMATIC HARDWARE PRINT] Discovered {} OS printers. Searching for physical/default printer...", availableServices.length);
                for (PrintService ps : availableServices) {
                    log.info("   -> OS Printer: '{}'", ps.getName());
                }
            }

            // 1. Check configured printer name
            if (configuredPrinterName != null && !configuredPrinterName.isBlank() && availableServices != null) {
                for (PrintService ps : availableServices) {
                    if (ps.getName().equalsIgnoreCase(configuredPrinterName) ||
                        ps.getName().toLowerCase().contains(configuredPrinterName.toLowerCase())) {
                        targetPrinter = ps;
                        log.info("[AUTOMATIC HARDWARE PRINT] Selected user-configured printer: '{}'", ps.getName());
                        break;
                    }
                }
            }

            // 2. Prioritize connected physical hardware printers (non-virtual)
            if (targetPrinter == null && availableServices != null) {
                for (PrintService ps : availableServices) {
                    String name = ps.getName().toLowerCase();
                    if (!name.contains("onenote") && !name.contains("fax") && !name.contains("xps") && !name.contains("pdf")) {
                        targetPrinter = ps;
                        log.info("[AUTOMATIC HARDWARE PRINT] Auto-detected physical hardware printer: '{}'", ps.getName());
                        break;
                    }
                }
            }

            // 3. Fallback to OS default printer
            if (targetPrinter == null) {
                try {
                    targetPrinter = PrintServiceLookup.lookupDefaultPrintService();
                    if (targetPrinter != null) {
                        log.info("[AUTOMATIC HARDWARE PRINT] Using Windows OS Default printer: '{}'", targetPrinter.getName());
                    }
                } catch (Throwable ignored) {}
            }

            // 4. Fallback to any available printer service
            if (targetPrinter == null && availableServices != null && availableServices.length > 0) {
                targetPrinter = availableServices[0];
            }

            if (targetPrinter != null) {
                log.info("[AUTOMATIC HARDWARE PRINT] Selected target printer: '{}'. Automatically spooling job {}...",
                        targetPrinter.getName(), job.getPublicReference());

                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPrintService(targetPrinter);
                int copies = (job.getCopies() != null && job.getCopies() > 0) ? job.getCopies() : 1;
                printerJob.setCopies(copies);
                printerJob.setJobName("DigiSeva_" + job.getPublicReference());

                javax.print.attribute.PrintRequestAttributeSet attributes = new javax.print.attribute.HashPrintRequestAttributeSet();
                attributes.add(new javax.print.attribute.standard.Copies(copies));

                // If printer is a virtual PDF printer and no physical printer name was set, supply Destination
                if (targetPrinter.getName().toLowerCase().contains("pdf") && (configuredPrinterName == null || configuredPrinterName.isBlank())) {
                    File spoolDir = new File(file.getParentFile(), "spool_prints");
                    if (!spoolDir.exists()) {
                        spoolDir.mkdirs();
                    }
                    File spoolFile = new File(spoolDir, "PRINTED_" + job.getPublicReference() + "_" + file.getName());
                    attributes.add(new javax.print.attribute.standard.Destination(spoolFile.toURI()));
                    log.info("[AUTOMATIC HARDWARE PRINT] Virtual PDF spool configured to: {}", spoolFile.getAbsolutePath());
                }

                String lowerName = job.getStoredFilename().toLowerCase();
                if (lowerName.endsWith(".pdf")) {
                    try (PDDocument document = Loader.loadPDF(file)) {
                        printerJob.setPageable(new PDFPageable(document));
                        printerJob.print(attributes);
                        log.info("[HARDWARE PRINT] Successfully sent PDF job {} ({} pages, {} copies) to printer '{}'!",
                                job.getPublicReference(), document.getNumberOfPages(), copies, targetPrinter.getName());
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "HARDWARE_PRINT_SUCCESS",
                                "Dispatched to hardware printer [" + targetPrinter.getName() + "], " + copies + " copies spooled."
                        ));
                    }
                } else {
                    // Image file
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        printerJob.setPrintable((graphics, pageFormat, pageIndex) -> {
                            if (pageIndex > 0) {
                                return Printable.NO_SUCH_PAGE;
                            }
                            Graphics2D g2d = (Graphics2D) graphics;
                            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                            double scaleX = pageFormat.getImageableWidth() / image.getWidth();
                            double scaleY = pageFormat.getImageableHeight() / image.getHeight();
                            double scale = Math.min(scaleX, scaleY);
                            g2d.scale(scale, scale);
                            g2d.drawImage(image, 0, 0, null);
                            return Printable.PAGE_EXISTS;
                        });
                        printerJob.print(attributes);
                        log.info("[HARDWARE PRINT] Successfully sent image job {} to printer '{}'!",
                                job.getPublicReference(), targetPrinter.getName());
                        kioskLogRepository.save(new KioskLog(
                                job.getKioskId(),
                                job.getPublicReference(),
                                "HARDWARE_PRINT_SUCCESS",
                                "Dispatched image to hardware printer [" + targetPrinter.getName() + "], " + copies + " copies spooled."
                        ));
                    }
                }
                return;
            }

            // Fallback: Windows Desktop print command
            if (file != null && java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                log.info("[HARDWARE PRINT] Fallback: Invoking Windows Desktop print command for {}", file.getName());
                java.awt.Desktop.getDesktop().print(file);
                kioskLogRepository.save(new KioskLog(
                        job.getKioskId(),
                        job.getPublicReference(),
                        "HARDWARE_PRINT_SUCCESS",
                        "Dispatched via Windows default printer handler."
                ));
            } else {
                log.warn("[HARDWARE PRINT] No printer found and Desktop print not supported.");
            }

        } catch (Throwable t) {
            log.warn("[HARDWARE PRINT] Physical printer dispatch error: {}. Browser print fallback is available.", t.getMessage());
            // Attempt Desktop print fallback if printerJob threw
            try {
                if (file != null && java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                    log.info("[HARDWARE PRINT] Attempting Windows Desktop print fallback...");
                    java.awt.Desktop.getDesktop().print(file);
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void retryJob(PrintJob job) {
        log.info("[PRINT DISPATCH] Retrying job {}", job.getPublicReference());
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
        log.info("[PRINT DISPATCH] Cancelling job {}", job.getPublicReference());
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
        try {
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrintService != null) {
                return "ONLINE (Hardware Connected: " + defaultPrintService.getName() + ")";
            }
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services != null && services.length > 0) {
                return "ONLINE (Printer Detected: " + services[0].getName() + ")";
            }
        } catch (Throwable ignored) {}
        return "ONLINE (Ready for Hardware & Browser Print)";
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
