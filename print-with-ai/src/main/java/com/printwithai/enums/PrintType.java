package com.printwithai.enums;

public enum PrintType {
    BLACK_AND_WHITE("Black & White", 2.00),
    COLOR("Colour", 8.00);

    private final String displayName;
    private final double defaultRate;

    PrintType(String displayName, double defaultRate) {
        this.displayName = displayName;
        this.defaultRate = defaultRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDefaultRate() {
        return defaultRate;
    }
}
