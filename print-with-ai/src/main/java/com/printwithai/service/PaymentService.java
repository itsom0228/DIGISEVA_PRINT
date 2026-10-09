package com.printwithai.service;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;

public interface PaymentService {

    PaymentTransaction processDemoPayment(String trackingToken, PaymentMode paymentMode, String simulatedUpiId);

    PaymentTransaction getTransaction(String transactionReference);
}
