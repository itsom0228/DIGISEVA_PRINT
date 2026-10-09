package com.printwithai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "template_downloads", indexes = {
        @Index(name = "idx_dl_template", columnList = "template_id"),
        @Index(name = "idx_dl_time", columnList = "downloaded_at")
})
public class TemplateDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ApplicationTemplate template;

    @Column(name = "downloaded_at", nullable = false)
    private LocalDateTime downloadedAt;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType = "DOWNLOAD"; // "DOWNLOAD", "PRINT_ORDER", "PREVIEW"

    @Column(name = "kiosk_id", length = 50)
    private String kioskId;

    public TemplateDownload() {
    }

    public TemplateDownload(ApplicationTemplate template, String actionType, String kioskId) {
        this.template = template;
        this.actionType = actionType;
        this.kioskId = kioskId;
        this.downloadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ApplicationTemplate template) {
        this.template = template;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(LocalDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getKioskId() {
        return kioskId;
    }

    public void setKioskId(String kioskId) {
        this.kioskId = kioskId;
    }
}
