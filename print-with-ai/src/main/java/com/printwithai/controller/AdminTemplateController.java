package com.printwithai.controller;

import com.printwithai.dto.AdminTemplateFormDto;
import com.printwithai.dto.TemplateSearchFilterDto;
import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.service.ApplicationTemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminTemplateController {

    private final ApplicationTemplateService templateService;

    public AdminTemplateController(ApplicationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/templates")
    public String listTemplates(@ModelAttribute("filter") TemplateSearchFilterDto filter, Model model) {
        filter.setSize(30);
        Page<ApplicationTemplate> templates = templateService.searchTemplates(filter);
        List<OfficeCategory> categories = templateService.getAllCategories();

        model.addAttribute("templates", templates);
        model.addAttribute("categories", categories);
        model.addAttribute("filter", filter);
        return "admin/templates";
    }

    @GetMapping("/templates/new")
    public String newTemplateForm(Model model) {
        AdminTemplateFormDto form = new AdminTemplateFormDto();
        form.setLanguage("en");
        form.setActive(true);
        form.setVerified(true);

        model.addAttribute("form", form);
        model.addAttribute("categories", templateService.getAllCategories());
        model.addAttribute("isEdit", false);
        return "admin/template-form";
    }

    @PostMapping("/templates/new")
    public String createTemplate(@Valid @ModelAttribute("form") AdminTemplateFormDto form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", templateService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/template-form";
        }

        try {
            ApplicationTemplate created = templateService.createTemplate(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application template '" + created.getTitle() + "' created successfully!");
            return "redirect:/admin/templates";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error creating template: " + e.getMessage());
            model.addAttribute("categories", templateService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/template-form";
        }
    }

    @GetMapping("/templates/edit/{id}")
    public String editTemplateForm(@PathVariable("id") Long id, Model model) {
        ApplicationTemplate template = templateService.getTemplateById(id);

        AdminTemplateFormDto form = new AdminTemplateFormDto();
        form.setId(template.getId());
        form.setTitle(template.getTitle());
        form.setSlug(template.getSlug());
        form.setDescription(template.getDescription());
        form.setPurpose(template.getPurpose());
        form.setLanguage(template.getLanguage());
        form.setCategoryId(template.getCategory().getId());
        form.setTemplateContent(template.getTemplateContent());
        form.setOfficialTemplate(template.isOfficialTemplate());
        form.setVerified(template.isVerified());
        form.setSourceName(template.getSourceName());
        form.setSourceUrl(template.getSourceUrl());
        form.setInstructions(template.getInstructions());
        form.setRequiredDocuments(template.getRequiredDocuments());
        form.setActive(template.isActive());

        model.addAttribute("form", form);
        model.addAttribute("categories", templateService.getAllCategories());
        model.addAttribute("isEdit", true);
        return "admin/template-form";
    }

    @PostMapping("/templates/edit/{id}")
    public String updateTemplate(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("form") AdminTemplateFormDto form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", templateService.getAllCategories());
            model.addAttribute("isEdit", true);
            return "admin/template-form";
        }

        try {
            ApplicationTemplate updated = templateService.updateTemplate(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application template '" + updated.getTitle() + "' updated successfully!");
            return "redirect:/admin/templates";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating template: " + e.getMessage());
            model.addAttribute("categories", templateService.getAllCategories());
            model.addAttribute("isEdit", true);
            return "admin/template-form";
        }
    }

    @PostMapping("/templates/toggle/{id}")
    public String toggleTemplate(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            templateService.toggleTemplateActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Template status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/templates";
    }

    @PostMapping("/templates/delete/{id}")
    public String deleteTemplate(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            templateService.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Template deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/templates";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        List<OfficeCategory> categories = templateService.getAllCategories();
        model.addAttribute("categories", categories);
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@RequestParam(value = "id", required = false) Long id,
                               @RequestParam("name") String name,
                               @RequestParam("slug") String slug,
                               @RequestParam("description") String description,
                               @RequestParam("icon") String icon,
                               @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder,
                               RedirectAttributes redirectAttributes) {
        try {
            OfficeCategory category;
            if (id != null) {
                category = templateService.getCategoryById(id);
            } else {
                category = new OfficeCategory();
            }
            category.setName(name);
            category.setSlug(slug);
            category.setDescription(description);
            category.setIcon(icon);
            category.setDisplayOrder(displayOrder);

            templateService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
