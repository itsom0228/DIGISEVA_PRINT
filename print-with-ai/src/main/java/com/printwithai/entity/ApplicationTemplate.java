package com.printwithai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application_templates", indexes = {
        @Index(name = "idx_tmpl_slug", columnList = "slug"),
        @Index(name = "idx_tmpl_lang", columnList = "language"),
        @Index(name = "idx_tmpl_active", columnList = "active"),
        @Index(name = "idx_tmpl_category", columnList = "category_id")
})
public class ApplicationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String purpose;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String language = "en"; // "en", "mr", "hi"

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private OfficeCategory category;

    @Column(name = "template_content", columnDefinition = "TEXT", nullable = false)
    private String templateContent;

    @Column(name = "file_path", length = 300)
    private String filePath;

    @Column(name = "file_type", length = 50)
    private String fileType = "TEXT_TEMPLATE";

    @Column(name = "official_template", nullable = false)
    private boolean officialTemplate = false;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TemplateDownload> downloads = new ArrayList<>();

    public ApplicationTemplate() {
    }

    public ApplicationTemplate(String title, String slug, String description, String purpose,
                               String language, OfficeCategory category, String templateContent,
                               boolean officialTemplate, boolean verified) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.purpose = purpose;
        this.language = language;
        this.category = category;
        this.templateContent = templateContent;
        this.officialTemplate = officialTemplate;
        this.verified = verified;
        this.active = true;
        this.downloadCount = 0;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public OfficeCategory getCategory() {
        return category;
    }

    public void setCategory(OfficeCategory category) {
        this.category = category;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isOfficialTemplate() {
        return officialTemplate;
    }

    public void setOfficialTemplate(boolean officialTemplate) {
        this.officialTemplate = officialTemplate;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(String requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TemplateDownload> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<TemplateDownload> downloads) {
        this.downloads = downloads;
    }

    // Helper method for badge display
    public String getLanguageDisplayName() {
        if ("mr".equalsIgnoreCase(language)) return "मराठी (Marathi)";
        if ("hi".equalsIgnoreCase(language)) return "हिंदी (Hindi)";
        return "English";
    }
}
