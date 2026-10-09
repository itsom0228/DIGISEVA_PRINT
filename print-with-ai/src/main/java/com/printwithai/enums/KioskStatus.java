package com.printwithai.enums;

public enum KioskStatus {
    ACTIVE("Active"),
    MAINTENANCE("Maintenance"),
    OFFLINE("Offline");

    private final String description;

    KioskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
