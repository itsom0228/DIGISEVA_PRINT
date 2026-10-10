package com.printwithai.controller;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.service.PaymentService;
import com.printwithai.service.PrintJobService;
import com.printwithai.service.QrCodeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/print")
public class PaymentController {

    private final PrintJobService printJobService;
    private final PaymentService paymentService;
    private final QrCodeService qrCodeService;

    @org.springframework.beans.factory.annotation.Value("${app.payment.upi.vpa:digisevaprint@upi}")
    private String upiVpa;

    @org.springframework.beans.factory.annotation.Value("${app.payment.upi.payee-name:DigiSeva Print}")
    private String upiPayeeName;

    public PaymentController(PrintJobService printJobService, PaymentService paymentService, QrCodeService qrCodeService) {
        this.printJobService = printJobService;
        this.paymentService = paymentService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/payment/{trackingToken}")
    public String showPaymentPage(@PathVariable("trackingToken") String trackingToken, Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
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
            encodedPayee = "DigiSeva%20Print";
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

        // Generate high-resolution 300x300 QR Code with the exact UPI payload
        String upiQrBase64 = qrCodeService.generateQrCodeBase64(upiPayload, 300, 300);

        model.addAttribute("upiQrBase64", upiQrBase64);
        model.addAttribute("upiPayload", upiPayload);
        model.addAttribute("phonepePayload", phonepePayload);
        model.addAttribute("gpayPayload", gpayPayload);
        model.addAttribute("paytmPayload", paytmPayload);
        model.addAttribute("upiVpa", upiVpa);
        model.addAttribute("upiPayeeName", upiPayeeName);

        return "order/payment";
    }

    @PostMapping("/payment/{trackingToken}")
    public String processPayment(@PathVariable("trackingToken") String trackingToken,
                                 @RequestParam(value = "paymentMode", defaultValue = "DEMO_UPI") PaymentMode paymentMode,
                                 @RequestParam(value = "simulatedUpiId", required = false) String simulatedUpiId,
                                 RedirectAttributes redirectAttributes) {
        try {
            PaymentTransaction txn = paymentService.processDemoPayment(trackingToken, paymentMode, simulatedUpiId);
            redirectAttributes.addFlashAttribute("successMessage", "Demo payment of ₹" + txn.getAmount() + " processed successfully!");
            return "redirect:/print/confirmation/" + trackingToken;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/print/payment/" + trackingToken;
        }
    }

    @GetMapping("/confirmation/{trackingToken}")
    public String showConfirmationPage(@PathVariable("trackingToken") String trackingToken, Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
        model.addAttribute("job", job);
        return "order/confirmation";
    }
}
