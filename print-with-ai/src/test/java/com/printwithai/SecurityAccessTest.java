package com.printwithai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public home page should be accessible without credentials")
    void testPublicHomePageAccess() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public kiosk catalog should be accessible without credentials")
    void testPublicKioskAccess() throws Exception {
        mockMvc.perform(get("/kiosk"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public tracking search page should be accessible without credentials")
    void testPublicTrackSearchAccess() throws Exception {
        mockMvc.perform(get("/order/track"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/dashboard should redirect to /admin/login")
    void testAdminProtectedWithoutAuth() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Authenticated administrator should access /admin/dashboard")
    void testAdminAccessWithRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }
}
