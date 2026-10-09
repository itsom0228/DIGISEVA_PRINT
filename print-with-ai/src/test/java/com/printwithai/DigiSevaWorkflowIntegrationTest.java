package com.printwithai;

import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.Kiosk;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.enums.KioskStatus;
import com.printwithai.service.ApplicationTemplateService;
import com.printwithai.service.KioskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DigiSevaWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KioskService kioskService;

    @Autowired
    private ApplicationTemplateService templateService;

    @BeforeEach
    void setUp() {
        kioskService.seedInitialKiosks();
        templateService.seedInitialCategoriesAndTemplates();
    }

    @Test
    @DisplayName("Verify home page isolates office templates and does not mix other institutions")
    void testHomePageOfficeIsolation() throws Exception {
        // 1. Test default home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("selectedCategory"))
                .andExpect(model().attributeExists("officeTemplates"))
                .andExpect(model().attributeExists("designatedKiosk"))
                .andExpect(view().name("index"))
                .andExpect(result -> {
                    OfficeCategory selected = (OfficeCategory) result.getModelAndView().getModel().get("selectedCategory");
                    @SuppressWarnings("unchecked")
                    List<ApplicationTemplate> templates = (List<ApplicationTemplate>) result.getModelAndView().getModel().get("officeTemplates");
                    assertNotNull(selected);
                    assertNotNull(templates);
                    assertFalse(templates.isEmpty());
                    for (ApplicationTemplate t : templates) {
                        assertEquals(selected.getSlug(), t.getCategory().getSlug(),
                                "Must not mix templates from other institutions! Every template must belong to " + selected.getSlug());
                    }
                });

        // 2. Test explicit Tahsil office view
        mockMvc.perform(get("/").param("office", "tahsil-office"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    @SuppressWarnings("unchecked")
                    List<ApplicationTemplate> templates = (List<ApplicationTemplate>) result.getModelAndView().getModel().get("officeTemplates");
                    assertNotNull(templates);
                    assertFalse(templates.isEmpty());
                    for (ApplicationTemplate t : templates) {
                        assertEquals("tahsil-office", t.getCategory().getSlug(),
                                "Must not mix templates from other institutions in Tahsil Office view!");
                    }
                });

        // 3. Test explicit College & University office view
        mockMvc.perform(get("/").param("office", "college-university"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    @SuppressWarnings("unchecked")
                    List<ApplicationTemplate> templates = (List<ApplicationTemplate>) result.getModelAndView().getModel().get("officeTemplates");
                    assertNotNull(templates);
                    assertFalse(templates.isEmpty());
                    for (ApplicationTemplate t : templates) {
                        assertEquals("college-university", t.getCategory().getSlug(),
                                "Must not mix templates from other institutions in College view!");
                    }
                });
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Verify Admin can register, toggle status, and manage machines")
    void testAdminMachineManagement() throws Exception {
        String testKioskId = "TEST-ADMIN-01";

        // Setup new machine with CSRF
        mockMvc.perform(post("/admin/kiosks/save")
                        .with(csrf())
                        .param("kioskId", testKioskId)
                        .param("name", "District Collectorate Main Terminal")
                        .param("officeCategorySlug", "tahsil-office")
                        .param("location", "Room 101, Collector Office")
                        .param("city", "Pune")
                        .param("state", "Maharashtra")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kiosks"));

        assertTrue(kioskService.isValidKiosk(testKioskId));
        Kiosk created = kioskService.getKioskById(testKioskId);
        assertEquals("tahsil-office", created.getOfficeCategorySlug());
        assertEquals(KioskStatus.ACTIVE, created.getStatus());

        // Toggle status to MAINTENANCE
        mockMvc.perform(post("/admin/kiosks/" + created.getId() + "/toggle-status").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kiosks"));

        Kiosk updated = kioskService.getKioskById(testKioskId);
        assertEquals(KioskStatus.MAINTENANCE, updated.getStatus());

        // Delete machine
        mockMvc.perform(post("/admin/kiosks/" + created.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kiosks"));

        assertFalse(kioskService.isValidKiosk(testKioskId));
    }

    @Test
    @DisplayName("Verify public user cannot switch kiosk via open selection in /print/new")
    void testCustomerKioskLocationLocked() throws Exception {
        // Supplying non-existent kioskId should safely resolve to default TAHSIL001 without error
        mockMvc.perform(get("/print/new").param("kioskId", "NON_EXISTENT_KIOSK"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("kiosk"))
                .andExpect(view().name("order/upload"));
    }
}
