package com.printwithai.dto;

import com.printwithai.enums.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DemoPaymentRequest {

    @NotBlank(message = "Tracking token is required")
    private String trackingToken;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode = PaymentMode.DEMO_UPI;

    private String simulatedUpiId;

    public DemoPaymentRequest() {}

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getSimulatedUpiId() { return simulatedUpiId; }
    public void setSimulatedUpiId(String simulatedUpiId) { this.simulatedUpiId = simulatedUpiId; }
}
