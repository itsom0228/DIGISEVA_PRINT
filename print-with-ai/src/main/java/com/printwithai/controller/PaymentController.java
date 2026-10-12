package com.printwithai.controller;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.repository.PaymentTransactionRepository;
import com.printwithai.service.PaymentService;
import com.printwithai.service.PrintJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/print")
public class PaymentController {

    private final PrintJobService printJobService;
    private final PaymentService paymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @org.springframework.beans.factory.annotation.Value("${app.payment.upi.vpa:7820939263@ibl}")
    private String upiVpa;

    @org.springframework.beans.factory.annotation.Value("${app.payment.upi.payee-name:Omkar Vilas Sarpane}")
    private String upiPayeeName;

    @org.springframework.beans.factory.annotation.Value("${app.payment.operator-pin:1234}")
    private String operatorPin;

    public PaymentController(PrintJobService printJobService,
                             PaymentService paymentService,
                             PaymentTransactionRepository paymentTransactionRepository) {
        this.printJobService = printJobService;
        this.paymentService = paymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @GetMapping("/payment/{trackingToken}")
    public String showPaymentPage(@PathVariable("trackingToken") String trackingToken, Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            return "redirect:/print/confirmation/" + trackingToken;
        }

        // Check if customer already submitted UTR and is awaiting verification
        Optional<PaymentTransaction> pendingTxn = paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job);
        if (pendingTxn.isPresent() && pendingTxn.get().getStatus() == PaymentStatus.DEMO_PENDING
                && pendingTxn.get().getTransactionReference() != null
                && !pendingTxn.get().getTransactionReference().isBlank()
                && !pendingTxn.get().getTransactionReference().startsWith("TRK-")) {
            return "redirect:/print/payment/verifying/" + trackingToken;
        }

        model.addAttribute("job", job);
        model.addAttribute("paymentModes", PaymentMode.values());

        String amountStr = job.getTotalAmount().toPlainString();
        String orderRef = job.getPublicReference();
        String encodedPayee;
        String encodedNote;
        try {
            encodedPayee = java.net.URLEncoder.encode(upiPayeeName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
            encodedNote = java.net.URLEncoder.encode("DigiSeva Print " + orderRef, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            encodedPayee = "Omkar%20Vilas%20Sarpane";
            encodedNote = "Order_" + orderRef;
        }

        // Standard NPCI UPI URI with exact amount
        String upiPayload = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s&tr=%s",
                upiVpa, encodedPayee, amountStr, encodedNote, trackingToken);

        // App-specific direct intent URIs
        String phonepePayload = String.format("phonepe://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s&tr=%s",
                upiVpa, encodedPayee, amountStr, encodedNote, trackingToken);
        String gpayPayload = String.format("tez://upi/pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s&tr=%s",
                upiVpa, encodedPayee, amountStr, encodedNote, trackingToken);
        String paytmPayload = String.format("paytmmp://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s&tr=%s",
                upiVpa, encodedPayee, amountStr, encodedNote, trackingToken);

        model.addAttribute("upiPayload", upiPayload);
        model.addAttribute("phonepePayload", phonepePayload);
        model.addAttribute("gpayPayload", gpayPayload);
        model.addAttribute("paytmPayload", paytmPayload);
        model.addAttribute("upiVpa", upiVpa);
        model.addAttribute("upiPayeeName", upiPayeeName);

        return "order/payment";
    }

    /**
     * Customer submits their 12-digit UPI UTR / Transaction ID after making payment.
     * Crucially: This DOES NOT print the document. It puts the order into Awaiting Verification.
     */
    @PostMapping("/payment/{trackingToken}")
    public String processPayment(@PathVariable("trackingToken") String trackingToken,
                                 @RequestParam(value = "utrNumber", required = false) String utrNumber,
                                 @RequestParam(value = "simulatedUpiId", required = false) String simulatedUpiId,
                                 RedirectAttributes redirectAttributes) {
        String finalUtr = (utrNumber != null && !utrNumber.trim().isEmpty()) ? utrNumber.trim() : simulatedUpiId;
        if (finalUtr == null || finalUtr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "कृपया पेमेंट केल्यानंतर मिळालेला १२ अंकी UPI UTR / Transaction ID टाका.");
            return "redirect:/print/payment/" + trackingToken;
        }

        try {
            paymentService.submitPaymentVerification(trackingToken, finalUtr);
            redirectAttributes.addFlashAttribute("successMessage", "तुमचा UPI UTR (" + finalUtr + ") पडताळणीसाठी सबमिट झाला आहे.");
            return "redirect:/print/payment/verifying/" + trackingToken;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/print/payment/" + trackingToken;
        }
    }

    /**
     * Customer waiting / verification screen with live polling.
     */
    @GetMapping("/payment/verifying/{trackingToken}")
    public String showVerifyingPage(@PathVariable("trackingToken") String trackingToken, Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            return "redirect:/print/confirmation/" + trackingToken;
        }

        Optional<PaymentTransaction> txn = paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job);
        String utr = txn.map(PaymentTransaction::getTransactionReference).orElse("");

        model.addAttribute("job", job);
        model.addAttribute("utr", utr);
        model.addAttribute("upiVpa", upiVpa);
        model.addAttribute("upiPayeeName", upiPayeeName);

        return "order/verifying";
    }

    /**
     * API for frontend polling: Customer screen checks every 2s whether payment is verified.
     */
    @GetMapping("/api/payment-status/{trackingToken}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentLiveStatus(@PathVariable("trackingToken") String trackingToken) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);

        Map<String, Object> resp = new HashMap<>();
        resp.put("paid", job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS);
        resp.put("failed", job.getPaymentStatus() == PaymentStatus.DEMO_FAILED);
        resp.put("paymentStatus", job.getPaymentStatus().name());
        resp.put("jobStatus", job.getJobStatus().name());
        resp.put("failureReason", job.getFailureReason());
        resp.put("totalAmount", job.getTotalAmount().toPlainString());
        resp.put("publicReference", job.getPublicReference());

        Optional<PaymentTransaction> txn = paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job);
        resp.put("utr", txn.map(PaymentTransaction::getTransactionReference).orElse(""));

        return ResponseEntity.ok(resp);
    }

    /**
     * Operator / Machine Verification Endpoint:
     * When shopkeeper confirms exact payment on their PhonePe / UPI app, they click Verify & Print on the kiosk/machine.
     * Enforces amount cross-verification!
     */
    @PostMapping("/api/verify-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyAndReleasePrint(
            @RequestParam("trackingToken") String trackingToken,
            @RequestParam(value = "receivedAmount", required = false) BigDecimal receivedAmount,
            @RequestParam(value = "operatorPin", required = false) String pin) {

        if (pin != null && !pin.isBlank() && !pin.equals(this.operatorPin)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "चुकीचा ऑपरेटर पिन (Invalid Operator PIN)"));
        }

        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
        BigDecimal amountToCheck = (receivedAmount != null && receivedAmount.compareTo(BigDecimal.ZERO) > 0)
                ? receivedAmount
                : job.getTotalAmount();

        try {
            PaymentTransaction txn = paymentService.verifyAndApprovePayment(trackingToken, amountToCheck, "Kiosk Operator");
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "पेमेंट पडताळणी यशस्वी! ₹" + txn.getAmount() + " प्राप्त. प्रिंटिंग सुरू झाले आहे.");
            result.put("reference", job.getPublicReference());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Operator Rejection Endpoint:
     * Used when customer only paid ₹1 instead of ₹2, or sent invalid UTR.
     */
    @PostMapping("/api/reject-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectPayment(
            @RequestParam("trackingToken") String trackingToken,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "operatorPin", required = false) String pin) {

        if (pin != null && !pin.isBlank() && !pin.equals(this.operatorPin)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "चुकीचा ऑपरेटर पिन (Invalid Operator PIN)"));
        }

        String finalReason = (reason != null && !reason.isBlank()) ? reason : "अपूर्ण किंवा चुकीचे पेमेंट (Insufficient payment)";

        try {
            paymentService.rejectPayment(trackingToken, finalReason);
            return ResponseEntity.ok(Map.of("success", true, "message", "पेमेंट नाकारले गेले. प्रिंट ब्लॉक केली आहे."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Automated Webhook / SMS Notification Endpoint:
     * An Android SMS/UPI Forwarder app on the owner's phone can POST to this endpoint.
     */
    @PostMapping("/api/upi-webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> upiNotificationWebhook(@RequestBody Map<String, Object> payload) {
        String utr = (String) payload.get("utr");
        Object amountObj = payload.get("amount");
        String trackingToken = (String) payload.get("trackingToken");

        if (utr == null || utr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing UTR"));
        }

        BigDecimal amount = BigDecimal.ZERO;
        if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            try {
                amount = new BigDecimal((String) amountObj);
            } catch (Exception ignored) {}
        }

        // Find matching job by trackingToken or search pending transaction with this UTR
        PrintJob targetJob = null;
        if (trackingToken != null && !trackingToken.isBlank()) {
            targetJob = printJobService.getOrderByTrackingToken(trackingToken);
        } else {
            Optional<PaymentTransaction> txnOpt = paymentTransactionRepository.findByTransactionReference(utr.trim().toUpperCase());
            if (txnOpt.isPresent()) {
                targetJob = txnOpt.get().getPrintJob();
            }
        }

        if (targetJob == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No matching pending order found for UTR: " + utr));
        }

        try {
            paymentService.verifyAndApprovePayment(targetJob.getTrackingToken(), amount, "Automated UPI Webhook");
            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook verified and job printed!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/confirmation/{trackingToken}")
    public String showConfirmationPage(@PathVariable("trackingToken") String trackingToken, Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
        model.addAttribute("job", job);
        return "order/confirmation";
    }
}
