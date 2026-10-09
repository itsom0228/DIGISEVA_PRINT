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

        // Generate simulated UPI QR string
        String simulatedUpiPayload = String.format("upi://pay?pa=printwithai.demo@upi&pn=PrintWithAI_Kiosk&am=%s&cu=INR&tn=Order_%s",
                job.getTotalAmount().toPlainString(), job.getPublicReference());
        String upiQrBase64 = qrCodeService.generateQrCodeBase64(simulatedUpiPayload, 250, 250);
        model.addAttribute("upiQrBase64", upiQrBase64);

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
