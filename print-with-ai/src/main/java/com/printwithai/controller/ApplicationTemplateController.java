package com.printwithai.controller;

import com.printwithai.dto.TemplateCustomizationDto;
import com.printwithai.dto.TemplateSearchFilterDto;
import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.entity.PrintJob;
import com.printwithai.service.ApplicationTemplateService;
import com.printwithai.service.KioskService;
import com.printwithai.service.PdfTemplateGenerationService;
import com.printwithai.service.PrintJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/applications")
public class ApplicationTemplateController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationTemplateController.class);

    private final ApplicationTemplateService templateService;
    private final PdfTemplateGenerationService pdfGenerationService;
    private final PrintJobService printJobService;
    private final KioskService kioskService;

    public ApplicationTemplateController(ApplicationTemplateService templateService,
                                         PdfTemplateGenerationService pdfGenerationService,
                                         PrintJobService printJobService,
                                         KioskService kioskService) {
        this.templateService = templateService;
        this.pdfGenerationService = pdfGenerationService;
        this.printJobService = printJobService;
        this.kioskService = kioskService;
    }

    @GetMapping
    public String index(@ModelAttribute("filter") TemplateSearchFilterDto filter, Model model) {
        List<OfficeCategory> categories = templateService.getAllActiveCategoriesWithCounts();
        Page<ApplicationTemplate> templates = templateService.searchTemplates(filter);
        List<ApplicationTemplate> featured = templateService.getFeaturedTemplates();
        List<ApplicationTemplate> popular = templateService.getPopularTemplates();

        model.addAttribute("categories", categories);
        model.addAttribute("templates", templates);
        model.addAttribute("featured", featured);
        model.addAttribute("popular", popular);
        model.addAttribute("filter", filter);
        return "applications/index";
    }

    @GetMapping("/category/{slug}")
    public String viewCategory(@PathVariable("slug") String slug,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size,
                               Model model) {
        OfficeCategory category = templateService.getCategoryBySlug(slug);
        Page<ApplicationTemplate> templates = templateService.getTemplatesByCategory(slug, page, size);
        List<OfficeCategory> allCategories = templateService.getAllActiveCategoriesWithCounts();

        model.addAttribute("category", category);
        model.addAttribute("templates", templates);
        model.addAttribute("allCategories", allCategories);
        return "applications/category";
    }

    @GetMapping("/search")
    public String search(@ModelAttribute("filter") TemplateSearchFilterDto filter, Model model) {
        List<OfficeCategory> categories = templateService.getAllActiveCategoriesWithCounts();
        Page<ApplicationTemplate> templates = templateService.searchTemplates(filter);

        model.addAttribute("categories", categories);
        model.addAttribute("templates", templates);
        model.addAttribute("filter", filter);
        return "applications/index";
    }

    @GetMapping("/template/{slug}")
    public String viewTemplateDetails(@PathVariable("slug") String slug,
                                      @RequestParam(value = "kioskId", defaultValue = "LAPTOP001") String kioskId,
                                      Model model) {
        ApplicationTemplate template = templateService.getTemplateBySlug(slug);
        TemplateCustomizationDto customization = new TemplateCustomizationDto();
        customization.setKioskId(kioskId);
        customization.setRecipientTitle(template.getCategory().getName().contains("College") ? "The Principal" : "The Tahsildar / Officer");
        customization.setSubject("Application for " + template.getTitle());

        model.addAttribute("template", template);
        model.addAttribute("customization", customization);
        model.addAttribute("kiosks", kioskService.getAllKiosks());
        return "applications/details";
    }

    @PostMapping("/template/{id}/download")
    public ResponseEntity<Resource> downloadCustomizedPdf(@PathVariable("id") Long id,
                                                          @ModelAttribute TemplateCustomizationDto customization) {
        ApplicationTemplate template = templateService.getTemplateById(id);
        templateService.recordTemplateDownload(id, "DOWNLOAD", customization.getKioskId());

        byte[] pdfBytes = pdfGenerationService.generateApplicationPdf(template, customization);
        String filename = sanitizeFilename(template.getSlug() + "_customized.pdf");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(pdfBytes));
    }

    @GetMapping("/template/{id}/download")
    public ResponseEntity<Resource> downloadDefaultPdf(@PathVariable("id") Long id,
                                                       @RequestParam(value = "kioskId", defaultValue = "LAPTOP001") String kioskId) {
        ApplicationTemplate template = templateService.getTemplateById(id);
        templateService.recordTemplateDownload(id, "DOWNLOAD", kioskId);

        TemplateCustomizationDto emptyCustomization = new TemplateCustomizationDto();
        emptyCustomization.setKioskId(kioskId);
        byte[] pdfBytes = pdfGenerationService.generateApplicationPdf(template, emptyCustomization);
        String filename = sanitizeFilename(template.getSlug() + "_sample.pdf");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(pdfBytes));
    }

    @PostMapping("/template/{id}/print-order")
    public String printUsingPrintWithAi(@PathVariable("id") Long id,
                                        @ModelAttribute TemplateCustomizationDto customization,
                                        RedirectAttributes redirectAttributes) {
        try {
            ApplicationTemplate template = templateService.getTemplateById(id);
            templateService.recordTemplateDownload(id, "PRINT_ORDER", customization.getKioskId());

            byte[] pdfBytes = pdfGenerationService.generateApplicationPdf(template, customization);
            String docFilename = template.getSlug() + "_application.pdf";

            PrintJob job = printJobService.createOrderFromGeneratedDocument(
                    customization.getKioskId(),
                    pdfBytes,
                    docFilename,
                    template.getLanguage()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Application document prepared successfully! Please review printing configuration.");
            return "redirect:/print/configure/" + job.getTrackingToken();
        } catch (Exception e) {
            log.error("Failed to route template to print order workflow: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error preparing print job: " + e.getMessage());
            ApplicationTemplate template = templateService.getTemplateById(id);
            return "redirect:/applications/template/" + template.getSlug();
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
