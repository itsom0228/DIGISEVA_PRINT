package com.printwithai.service;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.enums.PaymentMode;
import java.math.BigDecimal;

public interface PaymentService {

    PaymentTransaction processDemoPayment(String trackingToken, PaymentMode paymentMode, String simulatedUpiId);

    PaymentTransaction submitPaymentVerification(String trackingToken, String utrNumber);

    PaymentTransaction verifyAndApprovePayment(String trackingToken, BigDecimal receivedAmount, String approvedBy);

    void rejectPayment(String trackingToken, String reason);

    PaymentTransaction getTransaction(String transactionReference);

    java.util.Optional<com.printwithai.entity.PrintJob> processIncomingNotification(String utr, BigDecimal amount, String sender, String rawText);
}
