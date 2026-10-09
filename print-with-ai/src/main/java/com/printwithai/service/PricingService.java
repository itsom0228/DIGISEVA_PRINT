package com.printwithai.service;

import com.printwithai.dto.PriceCalculationResponse;
import com.printwithai.enums.PrintType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {

    private final BigDecimal bwRatePerPage;
    private final BigDecimal colorRatePerPage;
    private final BigDecimal duplexDiscountPerSheet;

    public PricingService(
            @Value("${app.pricing.bw-per-page:2.00}") BigDecimal bwRatePerPage,
            @Value("${app.pricing.color-per-page:8.00}") BigDecimal colorRatePerPage,
            @Value("${app.pricing.duplex-discount:0.50}") BigDecimal duplexDiscountPerSheet) {
        this.bwRatePerPage = bwRatePerPage;
        this.colorRatePerPage = colorRatePerPage;
        this.duplexDiscountPerSheet = duplexDiscountPerSheet;
    }

    public BigDecimal getUnitPrice(PrintType printType) {
        if (printType == PrintType.COLOR) {
            return colorRatePerPage;
        }
        return bwRatePerPage;
    }

    public PriceCalculationResponse calculatePrice(int totalPages, int selectedPageCount, int copies,
                                                  PrintType printType, boolean duplex) {
        if (selectedPageCount <= 0) {
            selectedPageCount = 1;
        }
        if (copies <= 0) {
            copies = 1;
        }

        BigDecimal unitPrice = getUnitPrice(printType);

        // Base price = selected pages * copies * unit price
        BigDecimal rawPagesToPrint = BigDecimal.valueOf((long) selectedPageCount * copies);
        BigDecimal basePrice = unitPrice.multiply(rawPagesToPrint);

        BigDecimal discount = BigDecimal.ZERO;
        String duplexNote = "";

        if (duplex && selectedPageCount > 1) {
            // For duplex, paper sheets used = ceil(selectedPages / 2.0)
            int physicalSheetsPerCopy = (int) Math.ceil((double) selectedPageCount / 2.0);
            int sheetsSavedPerCopy = selectedPageCount - physicalSheetsPerCopy;
            if (sheetsSavedPerCopy > 0) {
                discount = duplexDiscountPerSheet
                        .multiply(BigDecimal.valueOf((long) sheetsSavedPerCopy * copies))
                        .setScale(2, RoundingMode.HALF_UP);
                duplexNote = String.format(" [Duplex paper savings applied: ₹%s]", discount.toPlainString());
            }
        }

        BigDecimal total = basePrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        String explanation = String.format("%d selected page(s) × %d copies × ₹%s/page = ₹%s%s (Demonstration Rate)",
                selectedPageCount, copies, unitPrice.toPlainString(), basePrice.toPlainString(), duplexNote);

        return new PriceCalculationResponse(
                totalPages,
                selectedPageCount,
                copies,
                printType,
                duplex,
                unitPrice,
                discount,
                total,
                explanation
        );
    }
}
