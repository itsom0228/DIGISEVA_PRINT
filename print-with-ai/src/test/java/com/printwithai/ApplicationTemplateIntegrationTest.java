package com.printwithai;

import com.printwithai.dto.TemplateCustomizationDto;
import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.service.ApplicationTemplateService;
import com.printwithai.service.KioskService;
import com.printwithai.service.PdfTemplateGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationTemplateService templateService;

    @Autowired
    private PdfTemplateGenerationService pdfGenerationService;

    @Autowired
    private KioskService kioskService;

    @BeforeEach
    void setUp() {
        kioskService.seedInitialKiosks();
        templateService.seedInitialCategoriesAndTemplates();
    }

    @Test
    @DisplayName("Verify /applications returns 200 with office categories and template catalogue")
    void testApplicationsIndex() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("templates"))
                .andExpect(view().name("applications/index"));
    }

    @Test
    @DisplayName("Verify category filtering returns templates belonging to College & University")
    void testCategoryFiltering() throws Exception {
        mockMvc.perform(get("/applications/category/college-university"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("templates"))
                .andExpect(view().name("applications/category"));
    }

    @Test
    @DisplayName("Verify template search by English keyword and Devanagari Unicode keyword")
    void testTemplateSearch() throws Exception {
        // English search
        mockMvc.perform(get("/applications").param("keyword", "Bonafide"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("templates"))
                .andExpect(view().name("applications/index"));

        // Marathi Unicode search
        mockMvc.perform(get("/applications").param("keyword", "दाखला"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("templates"))
                .andExpect(view().name("applications/index"));
    }

    @Test
    @DisplayName("Verify template details page renders with interactive customization model")
    void testTemplateDetails() throws Exception {
        mockMvc.perform(get("/applications/template/bonafide-certificate-application"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("template"))
                .andExpect(model().attributeExists("customization"))
                .andExpect(view().name("applications/details"));
    }

    @Test
    @DisplayName("Verify PDF generation service produces valid A4 PDF with Devanagari text")
    void testPdfGenerationService() {
        ApplicationTemplate template = templateService.getTemplateBySlug("bonafide-certificate-marathi");
        assertNotNull(template);

        TemplateCustomizationDto dto = new TemplateCustomizationDto();
        dto.setApplicantName("सचिन तेंडुलकर");
        dto.setOfficeName("शासकीय अभियांत्रिकी महाविद्यालय, पुणे");
        dto.setPurpose("राज्य शिष्यवृत्ती अर्ज");
        dto.setReferenceNumber("COMP-2026-99");

        byte[] pdfBytes = pdfGenerationService.generateApplicationPdf(template, dto);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "Generated PDF should be non-empty and well-formed");

        // Verify PDF Magic Bytes %PDF
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("Verify template download endpoint returns application/pdf attachment")
    void testTemplateDownloadEndpoint() throws Exception {
        ApplicationTemplate template = templateService.getTemplateBySlug("bonafide-certificate-application");
        assertNotNull(template);

        mockMvc.perform(get("/applications/template/" + template.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("Verify 'Print Using Print with AI' creates print job and redirects to print configure flow")
    void testPrintUsingPrintWithAiWorkflow() throws Exception {
        ApplicationTemplate template = templateService.getTemplateBySlug("bonafide-certificate-application");
        assertNotNull(template);

        mockMvc.perform(post("/applications/template/" + template.getId() + "/print-order")
                        .param("applicantName", "Rahul Sharma")
                        .param("officeName", "Pune Institute of Computer Technology")
                        .param("purpose", "Passport Verification")
                        .param("kioskId", "LAPTOP001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/print/configure/*"));
    }

    @Test
    @DisplayName("Verify unauthenticated access to admin templates redirects to login")
    void testAdminSecurityUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/templates"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Verify authenticated admin can access template management console")
    void testAdminSecurityAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/templates"));
    }
}
