package com.printwithai.controller;

import com.printwithai.entity.PrintJob;
import com.printwithai.exception.ResourceNotFoundException;
import com.printwithai.service.PrintJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderTrackingController {

    private final PrintJobService printJobService;

    public OrderTrackingController(PrintJobService printJobService) {
        this.printJobService = printJobService;
    }

    @GetMapping("/track")
    public String trackOrderPage(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token != null && !token.trim().isEmpty()) {
            return "redirect:/order/track/" + token.trim();
        }
        return "order/track-search";
    }

    @GetMapping("/track/{trackingToken}")
    public String viewOrderStatus(@PathVariable("trackingToken") String trackingToken, Model model) {
        try {
            PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
            model.addAttribute("job", job);
            return "order/track-status";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", "No active print order found matching token: " + trackingToken);
            return "order/track-search";
        }
    }

    @GetMapping("/api/status/{trackingToken}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveStatusApi(@PathVariable("trackingToken") String trackingToken) {
        try {
            PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
            Map<String, Object> resp = new HashMap<>();
            resp.put("publicReference", job.getPublicReference());
            resp.put("jobStatus", job.getJobStatus().name());
            resp.put("jobStatusDisplay", job.getJobStatus().getDisplayName());
            resp.put("stepNumber", job.getJobStatus().getStepNumber());
            resp.put("paymentStatus", job.getPaymentStatus().name());
            resp.put("isCompleted", job.getJobStatus().name().equals("COMPLETED"));
            resp.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
