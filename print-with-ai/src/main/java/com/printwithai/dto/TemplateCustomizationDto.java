package com.printwithai.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TemplateCustomizationDto {

    private String applicantName;
    private String contactNumber;
    private String address;
    private String recipientTitle;
    private String officeName;
    private String subject;
    private String referenceNumber; // Roll No / Application No / Account No
    private String purpose;
    private String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    private String customizedContent;
    private String kioskId = "LAPTOP001";

    public TemplateCustomizationDto() {
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRecipientTitle() {
        return recipientTitle;
    }

    public void setRecipientTitle(String recipientTitle) {
        this.recipientTitle = recipientTitle;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCustomizedContent() {
        return customizedContent;
    }

    public void setCustomizedContent(String customizedContent) {
        this.customizedContent = customizedContent;
    }

    public String getKioskId() {
        return (kioskId != null && !kioskId.trim().isEmpty()) ? kioskId.trim() : "LAPTOP001";
    }

    public void setKioskId(String kioskId) {
        this.kioskId = kioskId;
    }
}
