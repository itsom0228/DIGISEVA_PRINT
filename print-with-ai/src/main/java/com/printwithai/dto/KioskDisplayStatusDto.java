package com.printwithai.dto;

public class KioskDisplayStatusDto {

    private String kioskId;
    private String kioskName;
    private String location;
    private String availability = "ONLINE";

    // Stage: WAITING_FOR_CUSTOMER, CUSTOMER_CONNECTED, DOCUMENT_UPLOADED, VALIDATING_DOCUMENT,
    // PRINT_OPTIONS_CONFIRMED, AWAITING_PAYMENT, PAYMENT_CONFIRMED, QUEUED, PRINTING, COMPLETED, FAILED
    private String stage = "WAITING_FOR_CUSTOMER";
    private String stageTitle = "Waiting for Customer";
    private String stageDescription = "Scan the QR code with your phone to start printing.";
    private int progressPercent = 0;

    private boolean hasActiveOrder = false;
    private String orderReference;
    private String trackingToken;
    private String documentName;
    private int totalPages = 0;
    private int selectedPages = 0;
    private int copies = 1;
    private String printType;
    private String totalAmount;
    private String paymentStatus;
    private String jobStatus;
    private String failureReason;
    private boolean simulated = true;
    private String timestamp;

    public KioskDisplayStatusDto() {}

    // Getters and Setters
    public String getKioskId() { return kioskId; }
    public void setKioskId(String kioskId) { this.kioskId = kioskId; }

    public String getKioskName() { return kioskName; }
    public void setKioskName(String kioskName) { this.kioskName = kioskName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getStageTitle() { return stageTitle; }
    public void setStageTitle(String stageTitle) { this.stageTitle = stageTitle; }

    public String getStageDescription() { return stageDescription; }
    public void setStageDescription(String stageDescription) { this.stageDescription = stageDescription; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public boolean isHasActiveOrder() { return hasActiveOrder; }
    public void setHasActiveOrder(boolean hasActiveOrder) { this.hasActiveOrder = hasActiveOrder; }

    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getSelectedPages() { return selectedPages; }
    public void setSelectedPages(int selectedPages) { this.selectedPages = selectedPages; }

    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }

    public String getPrintType() { return printType; }
    public void setPrintType(String printType) { this.printType = printType; }

    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getJobStatus() { return jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
