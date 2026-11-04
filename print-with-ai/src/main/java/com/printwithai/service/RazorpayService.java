package com.printwithai.service;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;

public interface RazorpayService {

    /**
     * Creates a new Razorpay Order for the given print job.
     * @param job PrintJob to create order for
     * @return Razorpay Order ID (e.g. order_xxx)
     */
    String createOrder(PrintJob job);

    /**
     * Verifies the cryptographic payment signature returned by Razorpay Checkout
     * and automatically triggers printer release on success.
     */
    PaymentTransaction verifyAndProcessPayment(String trackingToken, String razorpayOrderId,
                                               String razorpayPaymentId, String razorpaySignature);

    /**
     * Verifies and processes Razorpay server-to-server webhook notifications.
     */
    void processWebhook(String payload, String signature);

    String getKeyId();
}

