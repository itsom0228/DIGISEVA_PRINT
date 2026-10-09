package com.printwithai.enums;

public enum PaymentMode {
    DEMO_UPI("Demo UPI (Simulated QR / App)"),
    DEMO_CASH("Demo Cash (Counter)");

    private final String description;

    PaymentMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
