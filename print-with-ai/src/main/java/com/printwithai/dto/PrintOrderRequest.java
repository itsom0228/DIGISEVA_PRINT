package com.printwithai.dto;

import com.printwithai.enums.PrintType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PrintOrderRequest {

    @NotBlank(message = "Kiosk identifier is required")
    private String kioskId;

    @NotNull(message = "Print type is required")
    private PrintType printType = PrintType.BLACK_AND_WHITE;

    @Min(value = 1, message = "Copies must be at least 1")
    private int copies = 1;

    private String pageSelectionMode = "ALL"; // ALL or CUSTOM

    private String customPageRange; // e.g., "1-3,5"

    private boolean duplex = false;

    public PrintOrderRequest() {}

    public String getKioskId() { return kioskId; }
    public void setKioskId(String kioskId) { this.kioskId = kioskId; }

    public PrintType getPrintType() { return printType; }
    public void setPrintType(PrintType printType) { this.printType = printType; }

    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }

    public String getPageSelectionMode() { return pageSelectionMode; }
    public void setPageSelectionMode(String pageSelectionMode) { this.pageSelectionMode = pageSelectionMode; }

    public String getCustomPageRange() { return customPageRange; }
    public void setCustomPageRange(String customPageRange) { this.customPageRange = customPageRange; }

    public boolean isDuplex() { return duplex; }
    public void setDuplex(boolean duplex) { this.duplex = duplex; }
}
