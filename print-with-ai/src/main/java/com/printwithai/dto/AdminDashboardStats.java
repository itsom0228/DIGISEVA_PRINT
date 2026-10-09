package com.printwithai.dto;

import java.math.BigDecimal;

public class AdminDashboardStats {

    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long failedOrders;
    private BigDecimal totalDemoSales;
    private long activeKiosks;

    public AdminDashboardStats() {
        this.totalDemoSales = BigDecimal.ZERO;
    }

    public AdminDashboardStats(long totalOrders, long pendingOrders, long completedOrders,
                               long failedOrders, BigDecimal totalDemoSales, long activeKiosks) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.completedOrders = completedOrders;
        this.failedOrders = failedOrders;
        this.totalDemoSales = totalDemoSales != null ? totalDemoSales : BigDecimal.ZERO;
        this.activeKiosks = activeKiosks;
    }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getFailedOrders() { return failedOrders; }
    public void setFailedOrders(long failedOrders) { this.failedOrders = failedOrders; }

    public BigDecimal getTotalDemoSales() { return totalDemoSales; }
    public void setTotalDemoSales(BigDecimal totalDemoSales) { this.totalDemoSales = totalDemoSales; }

    public long getActiveKiosks() { return activeKiosks; }
    public void setActiveKiosks(long activeKiosks) { this.activeKiosks = activeKiosks; }
}
