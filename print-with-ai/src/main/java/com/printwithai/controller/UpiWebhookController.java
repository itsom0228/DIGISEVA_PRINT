package com.printwithai.controller;

import com.printwithai.entity.PrintJob;
import com.printwithai.service.PaymentService;
import com.printwithai.util.UpiNotificationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class UpiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(UpiWebhookController.class);

    private final PaymentService paymentService;

    public UpiWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Automated Webhook / SMS Notification Endpoint for Option 3.
     * Supports multiple URL paths so Android notification forwarders (MacroDroid, Tasker, SMS Forwarder)
     * can use whichever URL they prefer:
     * - POST /api/payment/notification
     * - POST /api/payment/upi-webhook
     * - POST /api/upi-webhook
     * - POST /print/api/upi-webhook
     * - POST /print/api/payment/notification
     *
     * Accepts either structured JSON:
     *   { "utr": "428412345678", "amount": 2.00, "sender": "PhonePe" }
     * OR raw SMS / Notification text:
     *   { "text": "Received Rs 2.00 from XYZ. UTR: 428412345678" }
     *   { "message": "..." }
     *   { "body": "..." }
     */
    @PostMapping(value = {
            "/api/payment/notification",
            "/api/payment/upi-webhook",
            "/api/upi-webhook",
            "/print/api/upi-webhook",
            "/print/api/payment/notification"
    })
    public ResponseEntity<Map<String, Object>> handleUpiNotification(@RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payload cannot be empty"));
        }

        String utr = (String) payload.get("utr");
        Object amountObj = payload.get("amount");
        String sender = (String) payload.get("sender");
        String text = (String) payload.getOrDefault("text", payload.getOrDefault("message", payload.get("body")));

        BigDecimal amount = null;
        if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            try {
                amount = new BigDecimal((String) amountObj);
            } catch (Exception ignored) {}
        }

        // If utr or amount was not explicitly passed, parse from the raw notification/SMS text
        if ((utr == null || utr.isBlank() || amount == null) && text != null && !text.isBlank()) {
            UpiNotificationParser.ParsedNotification parsed = UpiNotificationParser.parse(text);
            if (utr == null || utr.isBlank()) {
                utr = parsed.getUtr();
            }
            if (amount == null) {
                amount = parsed.getAmount();
            }
        }

        if (utr == null || utr.isBlank()) {
            log.warn("[WEBHOOK] Received notification without parseable UTR. Raw text: {}", text);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Could not extract 12-digit UTR from payload. Please send 'utr' field or SMS text containing UTR."
            ));
        }

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        log.info("[WEBHOOK] Incoming payment notification: UTR={}, Amount=₹{}, Sender={}", utr, amount, sender);

        Optional<PrintJob> matchedJob = paymentService.processIncomingNotification(utr, amount, sender, text);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("utr", utr);
        response.put("amount", amount);

        if (matchedJob.isPresent()) {
            PrintJob job = matchedJob.get();
            response.put("status", "AUTO_VERIFIED_AND_PRINTED");
            response.put("orderReference", job.getPublicReference());
            response.put("message", "Order " + job.getPublicReference() + " automatically verified and sent to printer!");
            log.info("[WEBHOOK] Successfully auto-verified and released print for order {}", job.getPublicReference());
        } else {
            response.put("status", "SAVED_AWAITING_CUSTOMER");
            response.put("message", "Notification recorded. Will automatically release print once customer submits UTR.");
            log.info("[WEBHOOK] Notification recorded for UTR {}. Awaiting customer order submission.", utr);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Easy diagnostic test endpoint: Simulates an SMS notification without an actual Android phone.
     * Example: GET /api/test-webhook?utr=428412345678&amount=2.00
     */
    @GetMapping(value = {"/api/test-webhook", "/api/payment/test-webhook", "/print/api/test-webhook"})
    public ResponseEntity<Map<String, Object>> testWebhookSimulation(
            @RequestParam(value = "utr", defaultValue = "TEST12345678") String utr,
            @RequestParam(value = "amount", defaultValue = "2.00") BigDecimal amount) {

        Optional<PrintJob> matchedJob = paymentService.processIncomingNotification(
                utr, amount, "Simulated Android Listener", "Test payment of ₹" + amount + " UTR: " + utr);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("utr", utr);
        resp.put("amount", amount);
        resp.put("matched", matchedJob.isPresent());
        matchedJob.ifPresent(j -> resp.put("orderReference", j.getPublicReference()));
        resp.put("message", matchedJob.isPresent()
                ? "Simulated match found! Order " + matchedJob.get().getPublicReference() + " verified and released to printer."
                : "Simulated payment recorded. Submit this UTR in customer order to auto-verify.");
        return ResponseEntity.ok(resp);
    }
}
