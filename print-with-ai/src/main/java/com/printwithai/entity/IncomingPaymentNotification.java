package com.printwithai.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incoming_payment_notifications", indexes = {
    @Index(name = "idx_incoming_utr", columnList = "utr"),
    @Index(name = "idx_incoming_created", columnList = "created_at")
})
public class IncomingPaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String utr;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String sender;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false)
    private Boolean matched = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IncomingPaymentNotification() {}

    public IncomingPaymentNotification(String utr, BigDecimal amount, String sender, String rawText) {
        this.utr = utr;
        this.amount = amount;
        this.sender = sender;
        this.rawText = rawText;
        this.matched = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Boolean getMatched() { return matched; }
    public void setMatched(Boolean matched) { this.matched = matched; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
