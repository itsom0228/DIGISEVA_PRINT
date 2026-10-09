package com.printwithai.enums;

public enum PrintJobStatus {
    CREATED("Order Created", 1),
    AWAITING_PAYMENT("Awaiting Payment", 2),
    QUEUED("Queued for Printing", 3),
    PRINTING("Printing in Progress", 4),
    COMPLETED("Printing Completed", 5),
    FAILED("Print Job Failed", 0),
    CANCELLED("Order Cancelled", 0);

    private final String displayName;
    private final int stepNumber;

    PrintJobStatus(String displayName, int stepNumber) {
        this.displayName = displayName;
        this.stepNumber = stepNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
