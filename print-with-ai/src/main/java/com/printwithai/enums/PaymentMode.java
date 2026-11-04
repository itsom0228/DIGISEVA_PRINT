package com.printwithai.enums;

public enum PaymentMode {
    RAZORPAY("Razorpay (UPI / QR / Cards)"),
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
