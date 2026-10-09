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

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PrintJobService printJobService;
    private final KioskService kioskService;
    private final QrCodeService qrCodeService;
    private final FileCleanupService fileCleanupService;

    public AdminController(
            PrintJobService printJobService,
            KioskService kioskService,
            QrCodeService qrCodeService,
            FileCleanupService fileCleanupService) {
        this.printJobService = printJobService;
        this.kioskService = kioskService;
        this.qrCodeService = qrCodeService;
        this.fileCleanupService = fileCleanupService;
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

    @PostMapping("/cleanup")
    public String triggerCleanup(RedirectAttributes redirectAttributes) {
        int purged = fileCleanupService.cleanupExpiredDocuments();
        redirectAttributes.addFlashAttribute("successMessage", "File privacy purge complete: " + purged + " expired files deleted from disk.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/kiosks")
    public String manageKiosks(Model model) {
        model.addAttribute("kiosks", kioskService.getAllKiosks());
        model.addAttribute("baseUrl", qrCodeService.getBaseUrl());
        return "admin/kiosks";
    }
}
