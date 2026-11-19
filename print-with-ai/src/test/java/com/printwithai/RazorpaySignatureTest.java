package com.printwithai;

import com.printwithai.service.impl.RazorpayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RazorpaySignatureTest {

    private RazorpayServiceImpl razorpayService;
    private final String testKeySecret = "yE8GUyjt5kQxlDlyTOuV8735";

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayServiceImpl(null, null, null, null);
        ReflectionTestUtils.setField(razorpayService, "keyId", "rzp_test_U1tg1HAd3f3eex");
        ReflectionTestUtils.setField(razorpayService, "keySecret", testKeySecret);
    }

    private String calculateExpectedSignature(String orderId, String paymentId, String secret) throws Exception {
        String data = orderId + "|" + paymentId;
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKey);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    @DisplayName("Verify HMAC-SHA256 signature with matching signature returns true")
    void testValidSignature() throws Exception {
        String orderId = "order_Tw7Aopj22RE8Hn";
        String paymentId = "pay_Tw7B123456789";
        String expectedSignature = calculateExpectedSignature(orderId, paymentId, testKeySecret);

        boolean result = razorpayService.verifySignatureOnly(orderId, paymentId, expectedSignature);
        assertTrue(result, "Expected signature verification to succeed");
    }

    @Test
    @DisplayName("Verify HMAC-SHA256 signature with tampered signature returns false")
    void testTamperedSignature() throws Exception {
        String orderId = "order_Tw7Aopj22RE8Hn";
        String paymentId = "pay_Tw7B123456789";
        String expectedSignature = calculateExpectedSignature(orderId, paymentId, testKeySecret);
        String tamperedSignature = expectedSignature.substring(0, expectedSignature.length() - 2) + "aa";

        boolean result = razorpayService.verifySignatureOnly(orderId, paymentId, tamperedSignature);
        assertFalse(result, "Expected tampered signature verification to fail");
    }

    @Test
    @DisplayName("Verify HMAC-SHA256 signature with tampered orderId returns false")
    void testTamperedOrderId() throws Exception {
        String orderId = "order_Tw7Aopj22RE8Hn";
        String paymentId = "pay_Tw7B123456789";
        String expectedSignature = calculateExpectedSignature(orderId, paymentId, testKeySecret);

        boolean result = razorpayService.verifySignatureOnly("order_TAMPERED", paymentId, expectedSignature);
        assertFalse(result, "Expected signature verification with different order ID to fail");
    }

    @Test
    @DisplayName("Verify HMAC-SHA256 signature with null or blank fields returns false")
    void testNullOrBlankFields() {
        assertFalse(razorpayService.verifySignatureOnly(null, "pay_123", "sig"));
        assertFalse(razorpayService.verifySignatureOnly("order_123", null, "sig"));
        assertFalse(razorpayService.verifySignatureOnly("order_123", "pay_123", null));
        assertFalse(razorpayService.verifySignatureOnly("", "pay_123", "sig"));
        assertFalse(razorpayService.verifySignatureOnly("order_123", "  ", "sig"));
    }
}

