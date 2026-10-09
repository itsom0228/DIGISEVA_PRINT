package com.printwithai.controller;

import com.printwithai.dto.PriceCalculationResponse;
import com.printwithai.enums.PrintType;
import com.printwithai.service.PricingService;
import com.printwithai.util.PageRangeParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final PricingService pricingService;

    public ApiController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/price-estimate")
    public ResponseEntity<PriceCalculationResponse> calculatePriceEstimate(@RequestBody Map<String, Object> req) {
        try {
            int totalPages = Integer.parseInt(req.getOrDefault("totalPages", 1).toString());
            int copies = Integer.parseInt(req.getOrDefault("copies", 1).toString());
            String printTypeStr = req.getOrDefault("printType", "BLACK_AND_WHITE").toString();
            PrintType printType = PrintType.valueOf(printTypeStr);
            boolean duplex = Boolean.parseBoolean(req.getOrDefault("duplex", false).toString());
            String pageSelectionMode = req.getOrDefault("pageSelectionMode", "ALL").toString();
            String customRange = (String) req.get("customPageRange");

            int selectedPageCount = totalPages;
            if ("CUSTOM".equalsIgnoreCase(pageSelectionMode) && customRange != null && !customRange.trim().isEmpty()) {
                List<Integer> parsed = PageRangeParser.parse(customRange, totalPages);
                selectedPageCount = parsed.size();
            }

            PriceCalculationResponse response = pricingService.calculatePrice(totalPages, selectedPageCount, copies, printType, duplex);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
