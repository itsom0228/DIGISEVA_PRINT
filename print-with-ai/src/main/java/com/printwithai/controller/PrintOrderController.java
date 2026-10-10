package com.printwithai.controller;

import com.printwithai.entity.Kiosk;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PrintType;
import com.printwithai.exception.InvalidFileException;
import com.printwithai.service.KioskService;
import com.printwithai.service.PricingService;
import com.printwithai.service.PrintJobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

import com.printwithai.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/print")
public class PrintOrderController {

    private final PrintJobService printJobService;
    private final KioskService kioskService;
    private final PricingService pricingService;
    private final StorageService storageService;

    public PrintOrderController(PrintJobService printJobService,
                                KioskService kioskService,
                                PricingService pricingService,
                                StorageService storageService) {
        this.printJobService = printJobService;
        this.kioskService = kioskService;
        this.pricingService = pricingService;
        this.storageService = storageService;
    }

    @GetMapping("/new")
    public String newPrintOrder(@RequestParam(value = "kioskId", defaultValue = "TAHSIL001") String kioskId,
                                Model model) {
        String targetId = (kioskId != null && kioskService.isValidKiosk(kioskId.trim())) ? kioskId.trim() : "TAHSIL001";
        Kiosk kiosk = kioskService.getKioskById(targetId);
        kioskService.recordCustomerConnection(targetId);
        model.addAttribute("kiosk", kiosk);
        return "order/upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("kioskId") String kioskId,
                                   @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            Locale currentLocale = LocaleContextHolder.getLocale();
            String lang = currentLocale != null ? currentLocale.getLanguage() : "en";

            PrintJob job = printJobService.createOrderFromUpload(kioskId, file, lang);
            return "redirect:/print/configure/" + job.getTrackingToken();
        } catch (InvalidFileException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/print/new?kioskId=" + kioskId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
            return "redirect:/print/new?kioskId=" + kioskId;
        }
    }

    @GetMapping("/configure/{trackingToken}")
    public String configurePrintOrder(@PathVariable("trackingToken") String trackingToken,
                                      Model model) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
        model.addAttribute("job", job);
        model.addAttribute("printTypes", PrintType.values());
        model.addAttribute("bwRate", pricingService.getUnitPrice(PrintType.BLACK_AND_WHITE));
        model.addAttribute("colorRate", pricingService.getUnitPrice(PrintType.COLOR));
        return "order/configure";
    }

    @PostMapping("/configure/{trackingToken}")
    public String submitConfiguration(@PathVariable("trackingToken") String trackingToken,
                                      @RequestParam(value = "printType", defaultValue = "BLACK_AND_WHITE") PrintType printType,
                                      @RequestParam(value = "copies", defaultValue = "1") int copies,
                                      @RequestParam(value = "pageSelectionMode", defaultValue = "ALL") String pageSelectionMode,
                                      @RequestParam(value = "customPageRange", required = false) String customPageRange,
                                      @RequestParam(value = "duplex", defaultValue = "false") boolean duplex,
                                      RedirectAttributes redirectAttributes) {
        try {
            printJobService.updatePrintConfiguration(trackingToken, printType, copies, pageSelectionMode, customPageRange, duplex);
            return "redirect:/print/payment/" + trackingToken;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/print/configure/" + trackingToken;
        }
    }

    @GetMapping("/document/{trackingToken}")
    @ResponseBody
    public ResponseEntity<Resource> streamDocument(@PathVariable("trackingToken") String trackingToken) {
        PrintJob job = printJobService.getOrderByTrackingToken(trackingToken);
        Resource resource = storageService.loadAsResource(job.getStoredFilename());

        String filename = job.getOriginalFilename() != null ? job.getOriginalFilename() : "document.pdf";
        String lower = filename.toLowerCase();
        String contentType = "application/pdf";
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
