package com.printwithai.service;

import com.printwithai.dto.AdminTemplateFormDto;
import com.printwithai.dto.TemplateSearchFilterDto;
import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicationTemplateService {

    List<OfficeCategory> getAllActiveCategoriesWithCounts();

    List<OfficeCategory> getAllCategories();

    OfficeCategory getCategoryBySlug(String slug);

    OfficeCategory getCategoryById(Long id);

    OfficeCategory saveCategory(OfficeCategory category);

    Page<ApplicationTemplate> searchTemplates(TemplateSearchFilterDto filter);

    Page<ApplicationTemplate> getTemplatesByCategory(String categorySlug, int page, int size);

    ApplicationTemplate getTemplateBySlug(String slug);

    ApplicationTemplate getTemplateById(Long id);

    List<ApplicationTemplate> getFeaturedTemplates();

    List<ApplicationTemplate> getPopularTemplates();

    ApplicationTemplate createTemplate(AdminTemplateFormDto form);

    ApplicationTemplate updateTemplate(Long id, AdminTemplateFormDto form);

    void toggleTemplateActive(Long id);

    void deleteTemplate(Long id);

    void recordTemplateDownload(Long templateId, String actionType, String kioskId);

    void seedInitialCategoriesAndTemplates();
}
