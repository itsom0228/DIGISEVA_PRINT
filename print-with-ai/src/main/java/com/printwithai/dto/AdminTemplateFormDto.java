package com.printwithai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminTemplateFormDto {

    private Long id;

    @NotBlank(message = "Template title is required.")
    @Size(max = 200, message = "Title must not exceed 200 characters.")
    private String title;

    @NotBlank(message = "Slug is required.")
    @Size(max = 200, message = "Slug must not exceed 200 characters.")
    private String slug;

    @Size(max = 1000, message = "Description must not exceed 1000 characters.")
    private String description;

    @Size(max = 500, message = "Purpose must not exceed 500 characters.")
    private String purpose;

    @NotBlank(message = "Language is required.")
    private String language = "en";

    @NotNull(message = "Category is required.")
    private Long categoryId;

    @NotBlank(message = "Template text content is required.")
    private String templateContent;

    private boolean officialTemplate = false;

    private boolean verified = false;

    private String sourceName;

    private String sourceUrl;

    private String instructions;

    private String requiredDocuments;

    private boolean active = true;

    public AdminTemplateFormDto() {
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
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
}
