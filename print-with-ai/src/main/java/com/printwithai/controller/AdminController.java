package com.printwithai.controller;

import com.printwithai.dto.AdminDashboardStats;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.scheduler.FileCleanupService;
import com.printwithai.service.KioskService;
import com.printwithai.service.PrintJobService;
import com.printwithai.service.QrCodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.printwithai.entity.Kiosk;
import com.printwithai.repository.OfficeCategoryRepository;
import com.printwithai.service.PaymentService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PrintJobService printJobService;
    private final KioskService kioskService;
    private final QrCodeService qrCodeService;
    private final FileCleanupService fileCleanupService;
    private final OfficeCategoryRepository officeCategoryRepository;
    private final PaymentService paymentService;

    public AdminController(
            PrintJobService printJobService,
            KioskService kioskService,
            QrCodeService qrCodeService,
            FileCleanupService fileCleanupService,
            OfficeCategoryRepository officeCategoryRepository,
            PaymentService paymentService) {
        this.printJobService = printJobService;
        this.kioskService = kioskService;
        this.qrCodeService = qrCodeService;
        this.fileCleanupService = fileCleanupService;
        this.officeCategoryRepository = officeCategoryRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "jobStatus", required = false) PrintJobStatus jobStatus,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        AdminDashboardStats stats = printJobService.getDashboardStats();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<PrintJob> ordersPage = printJobService.searchOrders(query, jobStatus, paymentStatus, pageable);

        model.addAttribute("stats", stats);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("query", query);
        model.addAttribute("jobStatus", jobStatus);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("jobStatuses", PrintJobStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("recentLogs", printJobService.getRecentLogs());

        return "admin/dashboard";
    }

    @GetMapping("/order/{publicReference}")
    public String orderDetails(@PathVariable("publicReference") String publicReference, Model model) {
        PrintJob job = printJobService.getOrderByPublicReference(publicReference);
        model.addAttribute("job", job);
        return "admin/order-details";
    }

    @PostMapping("/order/{publicReference}/retry")
    public String retryJob(@PathVariable("publicReference") String publicReference, RedirectAttributes redirectAttributes) {
        try {
            printJobService.retryJob(publicReference);
            redirectAttributes.addFlashAttribute("successMessage", "Order " + publicReference + " has been queued for simulated retry.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not retry order: " + e.getMessage());
        }
        return "redirect:/admin/order/" + publicReference;
    }

    @PostMapping("/order/{publicReference}/cancel")
    public String cancelJob(@PathVariable("publicReference") String publicReference, RedirectAttributes redirectAttributes) {
        try {
            printJobService.cancelJob(publicReference);
            redirectAttributes.addFlashAttribute("successMessage", "Order " + publicReference + " cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not cancel order: " + e.getMessage());
        }
        return "redirect:/admin/order/" + publicReference;
    }

    @PostMapping("/order/{publicReference}/verify-payment")
    public String adminVerifyPayment(@PathVariable("publicReference") String publicReference,
                                     @RequestParam(value = "receivedAmount", required = false) java.math.BigDecimal receivedAmount,
                                     RedirectAttributes redirectAttributes) {
        try {
            PrintJob job = printJobService.getOrderByPublicReference(publicReference);
            java.math.BigDecimal amount = (receivedAmount != null && receivedAmount.compareTo(java.math.BigDecimal.ZERO) > 0)
                    ? receivedAmount
                    : job.getTotalAmount();
            paymentService.verifyAndApprovePayment(job.getTrackingToken(), amount, "Admin Portal");
            redirectAttributes.addFlashAttribute("successMessage", "Payment of ₹" + amount + " verified! Order dispatched to printer queue.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment verification failed: " + e.getMessage());
        }
        return "redirect:/admin/order/" + publicReference;
    }

    @PostMapping("/order/{publicReference}/reject-payment")
    public String adminRejectPayment(@PathVariable("publicReference") String publicReference,
                                     @RequestParam(value = "reason", required = false) String reason,
                                     RedirectAttributes redirectAttributes) {
        try {
            PrintJob job = printJobService.getOrderByPublicReference(publicReference);
            String finalReason = (reason != null && !reason.isBlank()) ? reason : "Admin rejected payment (Amount mismatch or invalid UTR)";
            paymentService.rejectPayment(job.getTrackingToken(), finalReason);
            redirectAttributes.addFlashAttribute("successMessage", "Payment rejected and print cancelled for order " + publicReference);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not reject payment: " + e.getMessage());
        }
        return "redirect:/admin/order/" + publicReference;
    }

    @PostMapping("/cleanup")
    public String triggerCleanup(RedirectAttributes redirectAttributes) {
        int purged = fileCleanupService.cleanupExpiredDocuments();
        redirectAttributes.addFlashAttribute("successMessage", "File privacy purge complete: " + purged + " expired files deleted from disk.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/kiosks")
    public String manageKiosks(Model model, jakarta.servlet.http.HttpServletRequest request) {
        java.util.List<Kiosk> kiosks = kioskService.getAllKiosks();
        model.addAttribute("kiosks", kiosks);

        String activeBaseUrl = qrCodeService.getBaseUrl();
        if (request != null) {
            String serverName = request.getServerName();
            if (serverName != null && !serverName.equals("localhost") && !serverName.equals("127.0.0.1")) {
                int port = request.getServerPort();
                String scheme = request.getScheme();
                activeBaseUrl = scheme + "://" + serverName + (port == 80 || port == 443 ? "" : ":" + port);
            }
        }
        model.addAttribute("baseUrl", activeBaseUrl);
        model.addAttribute("categories", officeCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("newKiosk", new Kiosk());

        java.util.Map<String, String> qrCodeBase64Map = new java.util.HashMap<>();
        java.util.Map<String, String> kioskUrlMap = new java.util.HashMap<>();
        for (Kiosk k : kiosks) {
            try {
                String targetUrl = activeBaseUrl + "/print/new?kioskId=" + k.getKioskId();
                qrCodeBase64Map.put(k.getKioskId(), qrCodeService.generateQrCodeBase64(targetUrl, 350, 350));
                kioskUrlMap.put(k.getKioskId(), targetUrl);
            } catch (Exception e) {
                qrCodeBase64Map.put(k.getKioskId(), "");
                kioskUrlMap.put(k.getKioskId(), "");
            }
        }
        model.addAttribute("qrCodeBase64Map", qrCodeBase64Map);
        model.addAttribute("kioskUrlMap", kioskUrlMap);
        return "admin/kiosks";
    }

    @PostMapping("/kiosks/save")
    public String saveKiosk(@ModelAttribute Kiosk kiosk, RedirectAttributes redirectAttributes) {
        try {
            if (kiosk == null || kiosk.getKioskId() == null || kiosk.getKioskId().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Machine Terminal ID is required.");
                return "redirect:/admin/kiosks";
            }
            kiosk.setKioskId(kiosk.getKioskId().trim().toUpperCase());
            if (kiosk.getName() == null || kiosk.getName().trim().isEmpty()) {
                kiosk.setName("DigiSeva Print Terminal " + kiosk.getKioskId());
            }
            if (kiosk.getLocation() == null || kiosk.getLocation().trim().isEmpty()) {
                kiosk.setLocation("Citizen Service Center");
            }
            kioskService.saveKiosk(kiosk);
            redirectAttributes.addFlashAttribute("successMessage", "Machine terminal '" + kiosk.getKioskId() + "' saved and configured successfully.");
        } catch (Throwable t) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving machine: " + t.getMessage());
        }
        return "redirect:/admin/kiosks";
    }

    @PostMapping("/kiosks/{id}/toggle-status")
    public String toggleKioskStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            kioskService.toggleKioskStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Machine operational status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/kiosks";
    }

    @PostMapping("/kiosks/{id}/delete")
    public String deleteKiosk(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            kioskService.deleteKiosk(id);
            redirectAttributes.addFlashAttribute("successMessage", "Machine decommissioned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting machine: " + e.getMessage());
        }
        return "redirect:/admin/kiosks";
    }
}
