package com.printwithai.service;

import com.printwithai.ai.DocumentAssistantService;
import com.printwithai.dto.AdminDashboardStats;
import com.printwithai.dto.AiAnalysisResult;
import com.printwithai.dto.PriceCalculationResponse;
import com.printwithai.entity.Kiosk;
import com.printwithai.entity.KioskLog;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.enums.PrintType;
import com.printwithai.exception.InvalidFileException;
import com.printwithai.exception.InvalidOrderStateException;
import com.printwithai.exception.ResourceNotFoundException;
import com.printwithai.printer.PrinterService;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.storage.StorageService;
import com.printwithai.util.PageRangeParser;
import com.printwithai.util.ReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

@Service
public class PrintJobService {

    private static final Logger log = LoggerFactory.getLogger(PrintJobService.class);

    private final PrintJobRepository printJobRepository;
    private final KioskService kioskService;
    private final StorageService storageService;
    private final DocumentAssistantService documentAssistantService;
    private final PricingService pricingService;
    private final PrinterService printerService;
    private final KioskLogRepository kioskLogRepository;

    public PrintJobService(
            PrintJobRepository printJobRepository,
            KioskService kioskService,
            StorageService storageService,
            DocumentAssistantService documentAssistantService,
            PricingService pricingService,
            PrinterService printerService,
            KioskLogRepository kioskLogRepository) {
        this.printJobRepository = printJobRepository;
        this.kioskService = kioskService;
        this.storageService = storageService;
        this.documentAssistantService = documentAssistantService;
        this.pricingService = pricingService;
        this.printerService = printerService;
        this.kioskLogRepository = kioskLogRepository;
    }

    @Transactional
    public PrintJob createOrderFromUpload(String kioskId, MultipartFile file, String language) {
        Kiosk kiosk = kioskService.getKioskById(kioskId);

        // Store file securely with randomized name outside static dir
        StorageService.StoredFileInfo storedFile = storageService.store(file);

        // Run AI Document Intelligence analysis
        Path filePath = storedFile.filePath();
        AiAnalysisResult analysis = documentAssistantService.analyzeDocument(filePath, storedFile.contentType(), language);

        int totalPages = analysis.getTotalPages();
        int selectedPageCount = totalPages;

        PrintJob job = new PrintJob();
        job.setPublicReference(ReferenceGenerator.generateOrderReference());
        job.setTrackingToken(ReferenceGenerator.generateTrackingToken());
        job.setKioskId(kiosk.getKioskId());
        job.setKioskName(kiosk.getName());
        job.setOriginalFilename(storedFile.originalFilename());
        job.setStoredFilename(storedFile.storedFilename());
        job.setFileType(storedFile.contentType());
        job.setFileSizeBytes(storedFile.sizeBytes());
        job.setTotalPages(totalPages);
        job.setSelectedPages("ALL");
        job.setSelectedPageCount(selectedPageCount);
        job.setPrintType(PrintType.BLACK_AND_WHITE);
        job.setCopies(1);
        job.setDuplex(false);

        // Calculate price on backend
        PriceCalculationResponse priceCalc = pricingService.calculatePrice(
                totalPages, selectedPageCount, 1, PrintType.BLACK_AND_WHITE, false);
        job.setUnitPrice(priceCalc.getUnitPrice());
        job.setDuplexDiscount(priceCalc.getDuplexDiscount());
        job.setTotalAmount(priceCalc.getTotalAmount());

        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.CREATED);
        job.setAiSummary(analysis.getSummaryText());

        if (analysis.isBlankWarning()) {
            job.setAiBlankPagesNote("Low text density found on pages: " + analysis.getLowDensityPageNumbers());
        }

        PrintJob savedJob = printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                kiosk.getKioskId(),
                savedJob.getPublicReference(),
                "ORDER_CREATED",
                "New print job initiated at " + kiosk.getName() + " with file: " + storedFile.originalFilename()
        ));

        log.info("Created new print job {} at kiosk {}", savedJob.getPublicReference(), kiosk.getKioskId());
        return savedJob;
    }

    @Transactional
    public PrintJob createOrderFromGeneratedDocument(String kioskId, byte[] pdfBytes, String originalFilename, String language) {
        Kiosk kiosk = kioskService.getKioskById(kioskId);
        kioskService.recordCustomerConnection(kioskId);

        StorageService.StoredFileInfo storedFile = storageService.storeBytes(pdfBytes, originalFilename, "application/pdf");
        Path filePath = storedFile.filePath();
        AiAnalysisResult analysis = documentAssistantService.analyzeDocument(filePath, storedFile.contentType(), language);

        int totalPages = Math.max(1, analysis.getTotalPages());
        int selectedPageCount = totalPages;

        PrintJob job = new PrintJob();
        job.setPublicReference(ReferenceGenerator.generateOrderReference());
        job.setTrackingToken(ReferenceGenerator.generateTrackingToken());
        job.setKioskId(kiosk.getKioskId());
        job.setKioskName(kiosk.getName());
        job.setOriginalFilename(storedFile.originalFilename());
        job.setStoredFilename(storedFile.storedFilename());
        job.setFileType(storedFile.contentType());
        job.setFileSizeBytes(storedFile.sizeBytes());
        job.setTotalPages(totalPages);
        job.setSelectedPages("ALL");
        job.setSelectedPageCount(selectedPageCount);
        job.setPrintType(PrintType.BLACK_AND_WHITE);
        job.setCopies(1);
        job.setDuplex(false);

        PriceCalculationResponse priceCalc = pricingService.calculatePrice(
                totalPages, selectedPageCount, 1, PrintType.BLACK_AND_WHITE, false);
        job.setUnitPrice(priceCalc.getUnitPrice());
        job.setDuplexDiscount(priceCalc.getDuplexDiscount());
        job.setTotalAmount(priceCalc.getTotalAmount());
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.CREATED);
        job.setAiSummary(analysis.getSummaryText());

        PrintJob savedJob = printJobRepository.save(job);
        kioskLogRepository.save(new KioskLog(
                kiosk.getKioskId(),
                savedJob.getPublicReference(),
                "ORDER_CREATED",
                "Template print job initiated at " + kiosk.getName() + " with document: " + storedFile.originalFilename()
        ));
        log.info("Created template print job {} for kiosk {}", savedJob.getPublicReference(), kiosk.getKioskId());
        return savedJob;
    }

    @Transactional
    public PrintJob updatePrintConfiguration(String trackingToken, PrintType printType, int copies,
                                            String pageSelectionMode, String customRange, boolean duplex) {
        PrintJob job = getOrderByTrackingToken(trackingToken);

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("Cannot modify configuration of an order that has already been paid.");
        }

        int totalPages = job.getTotalPages();
        List<Integer> parsedPages;
        String canonicalPages;

        if ("CUSTOM".equalsIgnoreCase(pageSelectionMode) && customRange != null && !customRange.trim().isEmpty()) {
            parsedPages = PageRangeParser.parse(customRange, totalPages);
            canonicalPages = PageRangeParser.toCanonicalString(parsedPages, totalPages);
        } else {
            parsedPages = PageRangeParser.parse("ALL", totalPages);
            canonicalPages = "ALL";
        }

        int selectedCount = parsedPages.size();
        job.setSelectedPages(canonicalPages);
        job.setSelectedPageCount(selectedCount);
        job.setPrintType(printType != null ? printType : PrintType.BLACK_AND_WHITE);
        job.setCopies(copies > 0 ? copies : 1);
        job.setDuplex(duplex);

        // Recalculate price on backend
        PriceCalculationResponse price = pricingService.calculatePrice(
                totalPages, selectedCount, job.getCopies(), job.getPrintType(), job.getDuplex());

        job.setUnitPrice(price.getUnitPrice());
        job.setDuplexDiscount(price.getDuplexDiscount());
        job.setTotalAmount(price.getTotalAmount());
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        return printJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public PrintJob getOrderByTrackingToken(String trackingToken) {
        return printJobRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found with tracking token: " + trackingToken));
    }

    @Transactional(readOnly = true)
    public PrintJob getOrderByPublicReference(String publicReference) {
        return printJobRepository.findByPublicReference(publicReference)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found with reference: " + publicReference));
    }

    @Transactional(readOnly = true)
    public Page<PrintJob> searchOrders(String query, PrintJobStatus jobStatus, PaymentStatus paymentStatus, Pageable pageable) {
        return printJobRepository.searchJobs(query, jobStatus, paymentStatus, pageable);
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long total = printJobRepository.count();
        long pending = printJobRepository.countByPaymentStatus(PaymentStatus.DEMO_PENDING);
        long completed = printJobRepository.countByJobStatus(PrintJobStatus.COMPLETED);
        long failed = printJobRepository.countByJobStatus(PrintJobStatus.FAILED);
        BigDecimal sales = printJobRepository.calculateTotalDemoSales();
        long activeKiosks = kioskService.getAllKiosks().size();

        return new AdminDashboardStats(total, pending, completed, failed, sales, activeKiosks);
    }

    @Transactional
    public void retryJob(String publicReference) {
        PrintJob job = getOrderByPublicReference(publicReference);
        if (job.getPaymentStatus() != PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("Cannot retry a job that has not completed demo payment.");
        }
        printerService.retryJob(job);
    }

    @Transactional
    public void cancelJob(String publicReference) {
        PrintJob job = getOrderByPublicReference(publicReference);
        if (job.getJobStatus() == PrintJobStatus.COMPLETED) {
            throw new InvalidOrderStateException("Cannot cancel an already completed job.");
        }
        printerService.cancelJob(job);
    }

    @Transactional(readOnly = true)
    public List<KioskLog> getRecentLogs() {
        return kioskLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public com.printwithai.dto.KioskDisplayStatusDto getKioskDisplayStatus(String kioskId) {
        Kiosk kiosk = kioskService.getKioskById(kioskId);
        com.printwithai.dto.KioskDisplayStatusDto dto = new com.printwithai.dto.KioskDisplayStatusDto();
        dto.setKioskId(kiosk.getKioskId());
        dto.setKioskName(kiosk.getName());
        dto.setLocation(kiosk.getLocation());
        dto.setAvailability(kiosk.getStatus().name());
        dto.setTimestamp(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a")));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.Optional<PrintJob> recentJobOpt = printJobRepository.findFirstByKioskIdOrderByCreatedAtDesc(kioskId);

        boolean showJob = false;
        if (recentJobOpt.isPresent()) {
            PrintJob job = recentJobOpt.get();
            PrintJobStatus status = job.getJobStatus();

            if (status == PrintJobStatus.COMPLETED) {
                java.time.LocalDateTime compTime = job.getCompletedAt() != null ? job.getCompletedAt() : job.getUpdatedAt();
                if (compTime != null && compTime.isAfter(now.minusSeconds(45))) {
                    showJob = true;
                    populateJobInDisplayDto(dto, job, "COMPLETED", "Printing Completed!",
                            "Document printed successfully. Please collect your document from the output tray.", 100);
                }
            } else if (status == PrintJobStatus.FAILED) {
                java.time.LocalDateTime failTime = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
                if (failTime != null && failTime.isAfter(now.minusSeconds(45))) {
                    showJob = true;
                    populateJobInDisplayDto(dto, job, "FAILED", "Print Job Failed",
                            job.getFailureReason() != null ? job.getFailureReason() : "A simulated hardware error occurred.", 0);
                }
            } else if (status == PrintJobStatus.CANCELLED) {
                java.time.LocalDateTime cancelTime = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
                if (cancelTime != null && cancelTime.isAfter(now.minusSeconds(25))) {
                    showJob = true;
                    populateJobInDisplayDto(dto, job, "FAILED", "Order Cancelled", "This print order was cancelled.", 0);
                }
            } else if (status == PrintJobStatus.PRINTING) {
                showJob = true;
                populateJobInDisplayDto(dto, job, "PRINTING", "Printing in Progress",
                        "Printer hardware active. Simulating paper feed & laser output...", 85);
            } else if (status == PrintJobStatus.QUEUED) {
                showJob = true;
                populateJobInDisplayDto(dto, job, "QUEUED", "Queued for Printing",
                        "Demo payment confirmed! Job queued in printer buffer...", 65);
            } else if (status == PrintJobStatus.AWAITING_PAYMENT) {
                showJob = true;
                populateJobInDisplayDto(dto, job, "AWAITING_PAYMENT", "Awaiting Demo Payment",
                        "Print options confirmed. Please complete the demo payment on your smartphone.", 45);
            } else if (status == PrintJobStatus.CREATED) {
                showJob = true;
                if (job.getCreatedAt() != null && job.getCreatedAt().isAfter(now.minusSeconds(4))) {
                    populateJobInDisplayDto(dto, job, "VALIDATING_DOCUMENT", "Validating Document",
                            "AI Document Assistant analyzing pages & structure...", 20);
                } else {
                    populateJobInDisplayDto(dto, job, "DOCUMENT_UPLOADED", "Document Uploaded",
                            "Document uploaded: " + job.getOriginalFilename() + ". Customer selecting options on phone...", 30);
                }
            }
        }

        if (!showJob) {
            java.time.Instant lastConnected = kioskService.getLastCustomerConnection(kioskId);
            if (lastConnected != null && lastConnected.isAfter(java.time.Instant.now().minusSeconds(45))) {
                dto.setStage("CUSTOMER_CONNECTED");
                dto.setStageTitle("Customer Connected");
                dto.setStageDescription("Smartphone connected! Waiting for customer to select and upload document...");
                dto.setProgressPercent(10);
            } else {
                dto.setStage("WAITING_FOR_CUSTOMER");
                dto.setStageTitle("Waiting for Customer");
                dto.setStageDescription("Scan the QR code with your phone to start printing.");
                dto.setProgressPercent(0);
            }
            dto.setHasActiveOrder(false);
        }

        return dto;
    }

    private void populateJobInDisplayDto(com.printwithai.dto.KioskDisplayStatusDto dto, PrintJob job,
                                         String stage, String title, String desc, int progress) {
        dto.setHasActiveOrder(true);
        dto.setStage(stage);
        dto.setStageTitle(title);
        dto.setStageDescription(desc);
        dto.setProgressPercent(progress);
        dto.setOrderReference(job.getPublicReference());
        dto.setTrackingToken(job.getTrackingToken());
        dto.setDocumentName(job.getOriginalFilename());
        dto.setTotalPages(job.getTotalPages());
        dto.setSelectedPages(job.getSelectedPageCount());
        dto.setCopies(job.getCopies());
        dto.setPrintType(job.getPrintType().getDisplayName());
        dto.setTotalAmount("₹" + job.getTotalAmount().toPlainString());
        dto.setPaymentStatus(job.getPaymentStatus().name());
        dto.setJobStatus(job.getJobStatus().name());
        dto.setFailureReason(job.getFailureReason());
        dto.setSimulated(true);
    }

    public void resetKioskDisplay(String kioskId) {
        kioskService.clearCustomerConnection(kioskId);
    }
}

