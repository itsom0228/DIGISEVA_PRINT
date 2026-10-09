package com.printwithai;

import com.printwithai.dto.KioskDisplayStatusDto;
import com.printwithai.entity.Kiosk;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.enums.PrintType;
import com.printwithai.service.KioskService;
import com.printwithai.service.PaymentService;
import com.printwithai.service.PrintJobService;
import com.printwithai.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KioskDisplayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KioskService kioskService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private PrintJobService printJobService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private com.printwithai.repository.PrintJobRepository printJobRepository;

    @BeforeEach
    void setUp() {
        printJobRepository.deleteAll();
        kioskService.seedInitialKiosks();
        kioskService.clearCustomerConnection("LAPTOP001");
    }

    @Test
    @DisplayName("Verify LAPTOP001 QR code generation produces valid non-empty image")
    void testLaptopKioskQrGeneration() {
        byte[] qrBytes = qrCodeService.generateKioskQrCode("LAPTOP001");
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 500, "QR byte length should be a valid PNG image size");

        String qrBase64 = qrCodeService.generateKioskQrCodeBase64("LAPTOP001");
        assertNotNull(qrBase64);
        assertFalse(qrBase64.isBlank());

        String kioskUrl = qrCodeService.getKioskUrl("LAPTOP001");
        assertTrue(kioskUrl.contains("/print/new?kioskId=LAPTOP001"));
    }

    @Test
    @DisplayName("Test Kiosk Display endpoint /kiosk/display loads successfully (200 OK)")
    void testKioskDisplayPageLoads() throws Exception {
        mockMvc.perform(get("/kiosk/display?kioskId=LAPTOP001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test Kiosk Display polling REST endpoint /kiosk/api/status/LAPTOP001")
    void testKioskDisplayPollingEndpoint() throws Exception {
        mockMvc.perform(get("/kiosk/api/status/LAPTOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kioskId").value("LAPTOP001"))
                .andExpect(jsonPath("$.availability").value("ACTIVE"))
                .andExpect(jsonPath("$.stage").exists());
    }

    @Test
    @DisplayName("Verify full customer flow & kiosk display status transitions on LAPTOP001")
    void testKioskToOrderStatusTransitions() {
        // Step 1: Customer visits /print/new -> records connection
        kioskService.recordCustomerConnection("LAPTOP001");
        KioskDisplayStatusDto initialStatus = printJobService.getKioskDisplayStatus("LAPTOP001");
        assertNotNull(initialStatus);
        assertEquals("CUSTOMER_CONNECTED", initialStatus.getStage());

        // Step 2: Customer uploads document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Farmer_Notice.pdf",
                "application/pdf",
                "%PDF-1.4 test document content for kiosk terminal".getBytes()
        );
        PrintJob job = printJobService.createOrderFromUpload("LAPTOP001", file, "en");
        assertNotNull(job);
        assertEquals("LAPTOP001", job.getKioskId());

        // Kiosk display should now show DOCUMENT_UPLOADED
        KioskDisplayStatusDto uploadStatus = printJobService.getKioskDisplayStatus("LAPTOP001");
        assertTrue(uploadStatus.isHasActiveOrder());
        assertEquals(job.getPublicReference(), uploadStatus.getOrderReference());
        assertTrue(uploadStatus.getStage().equals("DOCUMENT_UPLOADED") || uploadStatus.getStage().equals("VALIDATING_DOCUMENT"));

        // Step 3: Customer configures print options
        PrintJob configuredJob = printJobService.updatePrintConfiguration(
                job.getTrackingToken(), PrintType.COLOR, 2, "ALL", null, false);
        assertEquals(PrintJobStatus.AWAITING_PAYMENT, configuredJob.getJobStatus());

        KioskDisplayStatusDto optionsStatus = printJobService.getKioskDisplayStatus("LAPTOP001");
        assertEquals("AWAITING_PAYMENT", optionsStatus.getStage());
        assertEquals("Colour", optionsStatus.getPrintType());
        assertEquals(2, optionsStatus.getCopies());

        // Step 4: Demo payment completed
        paymentService.processDemoPayment(job.getTrackingToken(), PaymentMode.DEMO_UPI, "phonepe@upi");

        // Job moves to QUEUED or PRINTING
        PrintJob paidJob = printJobService.getOrderByTrackingToken(job.getTrackingToken());
        assertEquals(PaymentStatus.DEMO_SUCCESS, paidJob.getPaymentStatus());
        assertTrue(paidJob.getJobStatus() == PrintJobStatus.QUEUED || paidJob.getJobStatus() == PrintJobStatus.PRINTING);
    }

    @Test
    @DisplayName("Verify Kiosk Reset endpoint returns status to standby")
    void testKioskResetEndpoint() throws Exception {
        mockMvc.perform(post("/kiosk/api/reset/LAPTOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET"))
                .andExpect(jsonPath("$.kioskId").value("LAPTOP001"));
    }
}
