package com.printwithai;

import com.printwithai.dto.PriceCalculationResponse;
import com.printwithai.enums.PrintType;
import com.printwithai.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        // Rates: BW = 2.00, Color = 8.00, Duplex discount = 0.50 per sheet saved
        pricingService = new PricingService(BigDecimal.valueOf(2.00), BigDecimal.valueOf(8.00), BigDecimal.valueOf(0.50));
    }

    @Test
    @DisplayName("Calculate single copy Black & White price")
    void testBlackAndWhiteSingleCopyPrice() {
        PriceCalculationResponse response = pricingService.calculatePrice(5, 5, 1, PrintType.BLACK_AND_WHITE, false);

        assertNotNull(response);
        assertEquals(5, response.getSelectedPageCount());
        assertEquals(1, response.getCopies());
        assertEquals(PrintType.BLACK_AND_WHITE, response.getPrintType());
        // 5 pages * 1 copy * 2.00 = 10.00
        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(response.getTotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getDuplexDiscount()));
    }

    @Test
    @DisplayName("Calculate multiple copies Colour price")
    void testColourMultipleCopiesPrice() {
        // 3 selected pages * 4 copies * 8.00 = 96.00
        PriceCalculationResponse response = pricingService.calculatePrice(10, 3, 4, PrintType.COLOR, false);

        assertNotNull(response);
        assertEquals(3, response.getSelectedPageCount());
        assertEquals(4, response.getCopies());
        assertEquals(PrintType.COLOR, response.getPrintType());
        assertEquals(0, BigDecimal.valueOf(96.00).compareTo(response.getTotalAmount()));
    }

    @Test
    @DisplayName("Calculate Duplex printing discount")
    void testDuplexPrintingDiscount() {
        // 4 pages duplex: 2 physical sheets per copy (2 sheets saved per copy)
        // Base: 4 * 1 * 2.00 = 8.00. Discount = 2 sheets saved * 0.50 = 1.00. Total = 7.00.
        PriceCalculationResponse response = pricingService.calculatePrice(4, 4, 1, PrintType.BLACK_AND_WHITE, true);

        assertNotNull(response);
        assertTrue(response.isDuplex());
        assertEquals(0, BigDecimal.valueOf(1.00).compareTo(response.getDuplexDiscount()));
        assertEquals(0, BigDecimal.valueOf(7.00).compareTo(response.getTotalAmount()));
    }

    @Test
    @DisplayName("Handle edge cases with 0 or negative inputs gracefully")
    void testZeroOrNegativeInputs() {
        PriceCalculationResponse response = pricingService.calculatePrice(5, 0, 0, PrintType.BLACK_AND_WHITE, false);

        // Fallbacks to 1 page, 1 copy: 1 * 1 * 2.00 = 2.00
        assertEquals(0, BigDecimal.valueOf(2.00).compareTo(response.getTotalAmount()));
    }
}
