package com.printwithai.dto;

import com.printwithai.enums.PrintType;
import java.math.BigDecimal;

public class PriceCalculationResponse {

    private int totalPages;
    private int selectedPageCount;
    private int copies;
    private PrintType printType;
    private boolean duplex;
    private BigDecimal unitPrice;
    private BigDecimal duplexDiscount;
    private BigDecimal totalAmount;
    private String formattedAmount;
    private String formulaExplanation;

    public PriceCalculationResponse() {}

    public PriceCalculationResponse(int totalPages, int selectedPageCount, int copies, PrintType printType,
                                    boolean duplex, BigDecimal unitPrice, BigDecimal duplexDiscount,
                                    BigDecimal totalAmount, String formulaExplanation) {
        this.totalPages = totalPages;
        this.selectedPageCount = selectedPageCount;
        this.copies = copies;
        this.printType = printType;
        this.duplex = duplex;
        this.unitPrice = unitPrice;
        this.duplexDiscount = duplexDiscount;
        this.totalAmount = totalAmount;
        this.formattedAmount = "₹" + totalAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        this.formulaExplanation = formulaExplanation;
    }

    // Getters and Setters
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getSelectedPageCount() { return selectedPageCount; }
    public void setSelectedPageCount(int selectedPageCount) { this.selectedPageCount = selectedPageCount; }

    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }

    public PrintType getPrintType() { return printType; }
    public void setPrintType(PrintType printType) { this.printType = printType; }

    public boolean isDuplex() { return duplex; }
    public void setDuplex(boolean duplex) { this.duplex = duplex; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDuplexDiscount() { return duplexDiscount; }
    public void setDuplexDiscount(BigDecimal duplexDiscount) { this.duplexDiscount = duplexDiscount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getFormattedAmount() { return formattedAmount; }
    public void setFormattedAmount(String formattedAmount) { this.formattedAmount = formattedAmount; }

    public String getFormulaExplanation() { return formulaExplanation; }
    public void setFormulaExplanation(String formulaExplanation) { this.formulaExplanation = formulaExplanation; }
}
