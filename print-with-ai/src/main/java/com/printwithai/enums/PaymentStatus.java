package com.printwithai.enums;

public enum PaymentStatus {
    DEMO_PENDING("Demo Payment Pending"),
    DEMO_SUCCESS("Demo Payment Success"),
    DEMO_FAILED("Demo Payment Failed");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
