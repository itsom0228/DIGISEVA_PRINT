package com.printwithai.dto;

public class TemplateSearchFilterDto {

    private String keyword;
    private String category;
    private String language;
    private String fileType;
    private String sort = "popular"; // "popular", "recent", "title"
    private int page = 0;
    private int size = 12;

    public TemplateSearchFilterDto() {
    }

    public String getKeyword() {
        return (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return (category != null && !category.trim().isEmpty()) ? category.trim() : null;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLanguage() {
        return (language != null && !language.trim().isEmpty()) ? language.trim() : null;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFileType() {
        return (fileType != null && !fileType.trim().isEmpty()) ? fileType.trim() : null;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.max(1, size);
    }
}
