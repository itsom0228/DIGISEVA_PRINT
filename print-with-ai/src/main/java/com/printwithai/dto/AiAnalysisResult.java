package com.printwithai.dto;

import java.util.ArrayList;
import java.util.List;

public class AiAnalysisResult {

    private int totalPages;
    private int pagesWithText;
    private List<Integer> lowDensityPageNumbers = new ArrayList<>();
    private int estimatedWordCount;
    private boolean scannedOrImageHeavy;
    private String summaryText;
    private List<String> recommendations = new ArrayList<>();
    private String providerUsed; // "LOCAL_HEURISTIC" or "OLLAMA_NEURAL"
    private boolean blankWarning;

    public AiAnalysisResult() {}

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPagesWithText() { return pagesWithText; }
    public void setPagesWithText(int pagesWithText) { this.pagesWithText = pagesWithText; }

    public List<Integer> getLowDensityPageNumbers() { return lowDensityPageNumbers; }
    public void setLowDensityPageNumbers(List<Integer> lowDensityPageNumbers) { this.lowDensityPageNumbers = lowDensityPageNumbers; }

    public int getEstimatedWordCount() { return estimatedWordCount; }
    public void setEstimatedWordCount(int estimatedWordCount) { this.estimatedWordCount = estimatedWordCount; }

    public boolean isScannedOrImageHeavy() { return scannedOrImageHeavy; }
    public void setScannedOrImageHeavy(boolean scannedOrImageHeavy) { this.scannedOrImageHeavy = scannedOrImageHeavy; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public String getProviderUsed() { return providerUsed; }
    public void setProviderUsed(String providerUsed) { this.providerUsed = providerUsed; }

    public boolean isBlankWarning() { return blankWarning; }
    public void setBlankWarning(boolean blankWarning) { this.blankWarning = blankWarning; }
}
