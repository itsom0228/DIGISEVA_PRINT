package com.printwithai.service;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;

import java.util.Map;

public interface RazorpayService {

    /**
     * Creates a new Razorpay Order for the given print job.
     * @param job PrintJob to create order for
     * @return Razorpay Order ID (e.g. order_xxx)
     */
    String createOrder(PrintJob job);

    /**
     * Creates a new Razorpay Order with raw parameters.
     * Validates that amount >= 100 paise.
     * @param amountInPaise Amount in paise (e.g. 200 = ₹2.00)
     * @param currency Currency code (e.g. "INR")
     * @param receipt Receipt identifier
     * @return Map with keys: order_id, amount, currency, key_id
     */
    Map<String, Object> createOrderRaw(long amountInPaise, String currency, String receipt) throws Exception;

    /**
     * Cryptographically verifies HMAC-SHA256(order_id + "|" + payment_id, KEY_SECRET)
     * without side-effects.
     */
    boolean verifySignatureOnly(String orderId, String paymentId, String signature);

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

    String getKeySecret();
}

