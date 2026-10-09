package com.printwithai.entity;

import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.enums.PrintType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "print_jobs", indexes = {
    @Index(name = "idx_job_ref", columnList = "public_reference", unique = true),
    @Index(name = "idx_job_tracking", columnList = "tracking_token", unique = true),
    @Index(name = "idx_job_status", columnList = "job_status"),
    @Index(name = "idx_job_kiosk", columnList = "kiosk_id")
})
public class PrintJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_reference", nullable = false, unique = true, length = 32)
    private String publicReference;

    @Column(name = "tracking_token", nullable = false, unique = true, length = 64)
    private String trackingToken;

    @Column(name = "kiosk_id", nullable = false, length = 50)
    private String kioskId;

    @Column(name = "kiosk_name", length = 150)
    private String kioskName;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false)
    private Integer totalPages = 1;

    @Column(length = 200)
    private String selectedPages = "ALL";

    @Column(nullable = false)
    private Integer selectedPageCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrintType printType = PrintType.BLACK_AND_WHITE;

    @Column(nullable = false)
    private Integer copies = 1;

    @Column(nullable = false)
    private Boolean duplex = false;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.valueOf(2.00);

    @Column(precision = 10, scale = 2)
    private BigDecimal duplexDiscount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.valueOf(2.00);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.DEMO_PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 30)
    private PrintJobStatus jobStatus = PrintJobStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(length = 500)
    private String aiBlankPagesNote;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Boolean fileDeleted = false;

    private LocalDateTime fileDeletedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "printJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    public PrintJob() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublicReference() { return publicReference; }
    public void setPublicReference(String publicReference) { this.publicReference = publicReference; }

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getKioskId() { return kioskId; }
    public void setKioskId(String kioskId) { this.kioskId = kioskId; }

    public String getKioskName() { return kioskName; }
    public void setKioskName(String kioskName) { this.kioskName = kioskName; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    public String getSelectedPages() { return selectedPages; }
    public void setSelectedPages(String selectedPages) { this.selectedPages = selectedPages; }

    public Integer getSelectedPageCount() { return selectedPageCount; }
    public void setSelectedPageCount(Integer selectedPageCount) { this.selectedPageCount = selectedPageCount; }

    public PrintType getPrintType() { return printType; }
    public void setPrintType(PrintType printType) { this.printType = printType; }

    public Integer getCopies() { return copies; }
    public void setCopies(Integer copies) { this.copies = copies; }

    public Boolean getDuplex() { return duplex; }
    public void setDuplex(Boolean duplex) { this.duplex = duplex; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDuplexDiscount() { return duplexDiscount; }
    public void setDuplexDiscount(BigDecimal duplexDiscount) { this.duplexDiscount = duplexDiscount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public PrintJobStatus getJobStatus() { return jobStatus; }
    public void setJobStatus(PrintJobStatus jobStatus) { this.jobStatus = jobStatus; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiBlankPagesNote() { return aiBlankPagesNote; }
    public void setAiBlankPagesNote(String aiBlankPagesNote) { this.aiBlankPagesNote = aiBlankPagesNote; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Boolean getFileDeleted() { return fileDeleted; }
    public void setFileDeleted(Boolean fileDeleted) { this.fileDeleted = fileDeleted; }

    public LocalDateTime getFileDeletedAt() { return fileDeletedAt; }
    public void setFileDeletedAt(LocalDateTime fileDeletedAt) { this.fileDeletedAt = fileDeletedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<PaymentTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<PaymentTransaction> transactions) { this.transactions = transactions; }
}
