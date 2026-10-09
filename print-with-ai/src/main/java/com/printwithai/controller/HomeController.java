package com.printwithai.controller;

import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.Kiosk;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.repository.OfficeCategoryRepository;
import com.printwithai.service.ApplicationTemplateService;
import com.printwithai.service.KioskService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final KioskService kioskService;
    private final ApplicationTemplateService templateService;
    private final OfficeCategoryRepository officeCategoryRepository;

    public HomeController(KioskService kioskService,
                          ApplicationTemplateService templateService,
                          OfficeCategoryRepository officeCategoryRepository) {
        this.kioskService = kioskService;
        this.templateService = templateService;
        this.officeCategoryRepository = officeCategoryRepository;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "office", required = false) String officeSlug,
                       @RequestParam(value = "kioskId", required = false) String kioskId,
                       Model model) {
        List<OfficeCategory> categories = officeCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc();

        // Determine active office institution
        String selectedSlug = officeSlug;
        if (selectedSlug == null || selectedSlug.trim().isEmpty()) {
            if (kioskId != null && !kioskId.trim().isEmpty() && kioskService.isValidKiosk(kioskId.trim())) {
                Kiosk currentKiosk = kioskService.getKioskById(kioskId.trim());
                selectedSlug = currentKiosk.getOfficeCategorySlug();
            }
        }
        if (selectedSlug == null || selectedSlug.trim().isEmpty()) {
            selectedSlug = !categories.isEmpty() ? categories.get(0).getSlug() : "tahsil-office";
        }

        OfficeCategory selectedCategory = templateService.getCategoryBySlug(selectedSlug);

        // Strict isolation: Particular office consists ONLY of its related application forms
        Page<ApplicationTemplate> officeTemplates = templateService.getTemplatesByCategory(selectedSlug, 0, 50);

        // Designated terminal for this office
        Kiosk designatedKiosk = kioskService.getKioskForOfficeCategory(selectedSlug);

        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("officeTemplates", officeTemplates.getContent());
        model.addAttribute("designatedKiosk", designatedKiosk);

        return "index";
    }
}
