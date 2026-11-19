package com.printwithai;

import com.printwithai.controller.RazorpayApiController;
import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.service.RazorpayService;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RazorpayApiControllerTest {

    private StubRazorpayService stubService;
    private MockMvc mockMvc;

    static class StubRazorpayService implements RazorpayService {
        boolean shouldThrowAuthError = false;
        boolean shouldThrowApiError = false;
        boolean signatureValid = false;

        @Override
        public String createOrder(PrintJob job) {
            return "order_stub_123";
        }

        @Override
        public Map<String, Object> createOrderRaw(long amountInPaise, String currency, String receipt) throws Exception {
            if (shouldThrowAuthError) {
                throw new RazorpayException("401 Unauthorized: Invalid API key or secret");
            }
            if (shouldThrowApiError) {
                throw new RazorpayException("500 Internal Server Error from Razorpay");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("order_id", "order_test_12345");
            map.put("amount", amountInPaise);
            map.put("currency", currency != null ? currency : "INR");
            map.put("key_id", "rzp_test_U1tg1HAd3f3eex");
            return map;
        }

        @Override
        public boolean verifySignatureOnly(String orderId, String paymentId, String signature) {
            return signatureValid && "valid_signature_hash".equals(signature);
        }

        @Override
        public PaymentTransaction verifyAndProcessPayment(String trackingToken, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
            return null;
        }

        @Override
        public void processWebhook(String payload, String signature) {
        }

        @Override
        public String getKeyId() {
            return "rzp_test_U1tg1HAd3f3eex";
        }

        @Override
        public String getKeySecret() {
            return "yE8GUyjt5kQxlDlyTOuV8735";
        }
    }

    @BeforeEach
    void setUp() {
        stubService = new StubRazorpayService();
        RazorpayApiController controller = new RazorpayApiController(stubService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Create Order: Should return 400 when amount is missing")
    void testCreateOrderMissingAmount() throws Exception {
        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"INR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("Amount is required"));
    }

    @Test
    @DisplayName("Create Order: Should return 400 when amount is less than 100 paise")
    void testCreateOrderAmountLessThan100Paise() throws Exception {
        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 99, \"currency\": \"INR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("Amount must be at least 100 paise"));
    }

    @Test
    @DisplayName("Create Order: Should return 200 with order_id and amount when amount >= 100")
    void testCreateOrderSuccess() throws Exception {
        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 200, \"currency\": \"INR\", \"receipt\": \"rcpt_1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value("order_test_12345"))
                .andExpect(jsonPath("$.amount").value(200))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.key_id").value("rzp_test_U1tg1HAd3f3eex"));
    }

    @Test
    @DisplayName("Create Order: Should return 401 when Razorpay authentication fails")
    void testCreateOrderAuthFailure() throws Exception {
        stubService.shouldThrowAuthError = true;

        mockMvc.perform(post("/api/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500, \"currency\": \"INR\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("Razorpay authentication failed"));
    }

    @Test
    @DisplayName("Verify Payment: Should return 400 when required fields are missing")
    void testVerifyPaymentMissingFields() throws Exception {
        mockMvc.perform(post("/api/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razorpay_order_id\": \"order_123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failure"))
                .andExpect(jsonPath("$.error").value("Missing required payment fields"));
    }

    @Test
    @DisplayName("Verify Payment: Should return 400 on signature mismatch")
    void testVerifyPaymentSignatureMismatch() throws Exception {
        stubService.signatureValid = false;

        mockMvc.perform(post("/api/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razorpay_order_id\": \"order_123\", \"razorpay_payment_id\": \"pay_456\", \"razorpay_signature\": \"invalid_signature\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failure"))
                .andExpect(jsonPath("$.error").value("Invalid payment signature"));
    }

    @Test
    @DisplayName("Verify Payment: Should return 200 on valid signature match")
    void testVerifyPaymentSuccess() throws Exception {
        stubService.signatureValid = true;

        mockMvc.perform(post("/api/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razorpay_order_id\": \"order_123\", \"razorpay_payment_id\": \"pay_456\", \"razorpay_signature\": \"valid_signature_hash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Payment verified successfully"))
                .andExpect(jsonPath("$.order_id").value("order_123"))
                .andExpect(jsonPath("$.payment_id").value("pay_456"));
    }
}
