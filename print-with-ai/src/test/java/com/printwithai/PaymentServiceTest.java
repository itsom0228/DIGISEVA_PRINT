package com.printwithai;

import com.printwithai.entity.IncomingPaymentNotification;
import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.exception.InvalidOrderStateException;
import com.printwithai.printer.PrinterService;
import com.printwithai.repository.IncomingPaymentNotificationRepository;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PaymentTransactionRepository;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.service.impl.DemoPaymentServiceImpl;
import com.printwithai.util.UpiNotificationParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PrintJobRepository printJobRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PrinterService printerService;

    @Mock
    private KioskLogRepository kioskLogRepository;

    @Mock
    private IncomingPaymentNotificationRepository incomingPaymentNotificationRepository;

    private DemoPaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new DemoPaymentServiceImpl(
                printJobRepository,
                paymentTransactionRepository,
                printerService,
                kioskLogRepository,
                incomingPaymentNotificationRepository
        );
    }

    @Test
    @DisplayName("Successfully process demo payment and trigger simulated printer")
    void testProcessDemoPaymentSuccess() {
        PrintJob job = new PrintJob();
        job.setId(1L);
        job.setTrackingToken("TRK-TEST-123");
        job.setPublicReference("PAI-2026-TEST");
        job.setKioskId("TAHSIL001");
        job.setTotalAmount(BigDecimal.valueOf(10.00));
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        when(printJobRepository.findByTrackingToken("TRK-TEST-123")).thenReturn(Optional.of(job));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction txn = paymentService.processDemoPayment("TRK-TEST-123", PaymentMode.DEMO_UPI, "user@upi");

        assertNotNull(txn);
        assertEquals(PaymentStatus.DEMO_SUCCESS, txn.getStatus());
        assertEquals(PaymentStatus.DEMO_SUCCESS, job.getPaymentStatus());
        verify(printerService, times(1)).submitJob(job);
        verify(printJobRepository, times(1)).save(job);
    }

    @Test
    @DisplayName("Prevent duplicate payment on already paid job")
    void testPreventDuplicatePayment() {
        PrintJob job = new PrintJob();
        job.setTrackingToken("TRK-ALREADY-PAID");
        job.setPaymentStatus(PaymentStatus.DEMO_SUCCESS);

        when(printJobRepository.findByTrackingToken("TRK-ALREADY-PAID")).thenReturn(Optional.of(job));

        assertThrows(InvalidOrderStateException.class, () ->
                paymentService.processDemoPayment("TRK-ALREADY-PAID", PaymentMode.DEMO_UPI, null)
        );
        verify(printerService, never()).submitJob(any());
    }

    @Test
    @DisplayName("Submitting UTR puts job in verification pending and DOES NOT trigger printer without match")
    void testSubmitPaymentVerificationDoesNotPrint() {
        PrintJob job = new PrintJob();
        job.setId(2L);
        job.setTrackingToken("TRK-VERIFY-123");
        job.setPublicReference("PAI-2026-VERIFY");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00));
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        when(printJobRepository.findByTrackingToken("TRK-VERIFY-123")).thenReturn(Optional.of(job));
        when(paymentTransactionRepository.findByTransactionReference("428412345678")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(incomingPaymentNotificationRepository.findFirstByUtrOrderByCreatedAtDesc("428412345678")).thenReturn(Optional.empty());

        PaymentTransaction txn = paymentService.submitPaymentVerification("TRK-VERIFY-123", "428412345678");

        assertNotNull(txn);
        assertEquals("428412345678", txn.getTransactionReference());
        assertEquals(PaymentStatus.DEMO_PENDING, txn.getStatus());
        assertEquals(PaymentStatus.DEMO_PENDING, job.getPaymentStatus());
        // STRICT SECURITY ASSERTION: Printer MUST NOT be called!
        verify(printerService, never()).submitJob(any());
    }

    @Test
    @DisplayName("Option 3 Auto-Match: Submitting UTR auto-verifies and prints if SMS already arrived earlier")
    void testSubmitPaymentVerificationAutoMatchesExistingSms() {
        PrintJob job = new PrintJob();
        job.setId(2L);
        job.setTrackingToken("TRK-VERIFY-AUTO");
        job.setPublicReference("PAI-2026-AUTO");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00));
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        IncomingPaymentNotification smsNotif = new IncomingPaymentNotification(
                "428499998888", BigDecimal.valueOf(2.00), "PhonePe", "Received Rs 2.00 from Customer. UTR: 428499998888");
        smsNotif.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        when(printJobRepository.findByTrackingToken("TRK-VERIFY-AUTO")).thenReturn(Optional.of(job));
        when(paymentTransactionRepository.findByTransactionReference("428499998888")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(incomingPaymentNotificationRepository.findFirstByUtrOrderByCreatedAtDesc("428499998888")).thenReturn(Optional.of(smsNotif));

        PaymentTransaction txn = paymentService.submitPaymentVerification("TRK-VERIFY-AUTO", "428499998888");

        assertNotNull(txn);
        assertEquals(PaymentStatus.DEMO_SUCCESS, txn.getStatus());
        assertEquals(PaymentStatus.DEMO_SUCCESS, job.getPaymentStatus());
        assertTrue(smsNotif.getMatched());
        // AUTOMATED OPTION 3: Printer dispatched automatically!
        verify(printerService, times(1)).submitJob(job);
    }

    @Test
    @DisplayName("Option 3 Webhook: Incoming SMS automatically releases pending order and prints")
    void testProcessIncomingNotificationAutoVerifiesPendingJob() {
        PrintJob job = new PrintJob();
        job.setId(5L);
        job.setTrackingToken("TRK-WEBHOOK-123");
        job.setPublicReference("PAI-2026-WH");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00));
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        PaymentTransaction pendingTxn = new PaymentTransaction(
                job, "428477776666", PaymentMode.DEMO_UPI, BigDecimal.valueOf(2.00), PaymentStatus.DEMO_PENDING, "Awaiting"
        );

        when(paymentTransactionRepository.findByTransactionReference("428477776666")).thenReturn(Optional.of(pendingTxn));
        when(printJobRepository.findByTrackingToken("TRK-WEBHOOK-123")).thenReturn(Optional.of(job));
        when(paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)).thenReturn(Optional.of(pendingTxn));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        Optional<PrintJob> result = paymentService.processIncomingNotification(
                "428477776666", BigDecimal.valueOf(2.00), "PhonePe", "Received Rs 2.00 UTR: 428477776666");

        assertTrue(result.isPresent());
        assertEquals("PAI-2026-WH", result.get().getPublicReference());
        assertEquals(PaymentStatus.DEMO_SUCCESS, job.getPaymentStatus());
        verify(printerService, times(1)).submitJob(job);
    }

    @Test
    @DisplayName("Option 3 Webhook: Rejects payment and BLOCKS print if received amount is less than total amount (e.g. ₹1 instead of ₹2)")
    void testProcessIncomingNotificationRejectsInsufficientAmount() {
        PrintJob job = new PrintJob();
        job.setId(6L);
        job.setTrackingToken("TRK-UNDERPAID-123");
        job.setPublicReference("PAI-2026-UNDER");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00)); // Requires ₹2.00
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        PaymentTransaction pendingTxn = new PaymentTransaction(
                job, "428411112222", PaymentMode.DEMO_UPI, BigDecimal.valueOf(2.00), PaymentStatus.DEMO_PENDING, "Awaiting"
        );

        when(paymentTransactionRepository.findByTransactionReference("428411112222")).thenReturn(Optional.of(pendingTxn));
        when(printJobRepository.findByTrackingToken("TRK-UNDERPAID-123")).thenReturn(Optional.of(job));

        // Customer sent only ₹1.00!
        Optional<PrintJob> result = paymentService.processIncomingNotification(
                "428411112222", BigDecimal.valueOf(1.00), "PhonePe", "Received Rs 1.00 UTR: 428411112222");

        assertTrue(result.isPresent());
        assertEquals(PaymentStatus.DEMO_FAILED, job.getPaymentStatus());
        assertTrue(job.getFailureReason().contains("Insufficient payment"));
        // STRICT SECURITY ASSERTION: Printer is strictly blocked!
        verify(printerService, never()).submitJob(any());
    }

    @Test
    @DisplayName("Cross-verification REJECTS insufficient payment (₹1 paid instead of ₹2) and BLOCKS printer")
    void testCrossVerificationRejectsInsufficientPayment() {
        PrintJob job = new PrintJob();
        job.setId(3L);
        job.setTrackingToken("TRK-CHEATED-123");
        job.setPublicReference("PAI-2026-CHEAT");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00)); // Expected ₹2.00
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);

        when(printJobRepository.findByTrackingToken("TRK-CHEATED-123")).thenReturn(Optional.of(job));

        // Customer paid ₹1.00 when ₹2.00 was required!
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.verifyAndApprovePayment("TRK-CHEATED-123", BigDecimal.valueOf(1.00), "Kiosk Operator")
        );

        assertTrue(ex.getMessage().contains("अपूर्ण रक्कम"));
        assertEquals(PaymentStatus.DEMO_FAILED, job.getPaymentStatus());
        // STRICT SECURITY ASSERTION: Printer is strictly blocked!
        verify(printerService, never()).submitJob(any());
    }

    @Test
    @DisplayName("Cross-verification SUCCEEDS when full amount is paid and triggers printer")
    void testCrossVerificationSucceedsWithFullAmount() {
        PrintJob job = new PrintJob();
        job.setId(4L);
        job.setTrackingToken("TRK-HONEST-123");
        job.setPublicReference("PAI-2026-HONEST");
        job.setKioskId("LAPTOP001");
        job.setTotalAmount(BigDecimal.valueOf(2.00));
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setJobStatus(PrintJobStatus.AWAITING_PAYMENT);

        when(printJobRepository.findByTrackingToken("TRK-HONEST-123")).thenReturn(Optional.of(job));
        when(paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction txn = paymentService.verifyAndApprovePayment("TRK-HONEST-123", BigDecimal.valueOf(2.00), "Kiosk Operator");

        assertNotNull(txn);
        assertEquals(PaymentStatus.DEMO_SUCCESS, txn.getStatus());
        assertEquals(PaymentStatus.DEMO_SUCCESS, job.getPaymentStatus());
        verify(printerService, times(1)).submitJob(job);
    }

    @Test
    @DisplayName("UpiNotificationParser correctly extracts amount and 12-digit UTR from various SMS texts")
    void testUpiNotificationParser() {
        // PhonePe style SMS
        UpiNotificationParser.ParsedNotification p1 = UpiNotificationParser.parse(
                "Received Rs.2.00 from John Doe. UPI Ref: 428412345678. Check PhonePe.");
        assertEquals(new BigDecimal("2.00"), p1.getAmount());
        assertEquals("428412345678", p1.getUtr());

        // Google Pay style SMS
        UpiNotificationParser.ParsedNotification p2 = UpiNotificationParser.parse(
                "You received ₹50 from Ramesh (UPI Ref No 428498765432).");
        assertEquals(new BigDecimal("50"), p2.getAmount());
        assertEquals("428498765432", p2.getUtr());

        // Bank credited SMS
        UpiNotificationParser.ParsedNotification p3 = UpiNotificationParser.parse(
                "A/C *1234 credited by Rs 2.00 on 12-Oct-26 by UPI/428400112233/P2A/TRANSFER.");
        assertEquals(new BigDecimal("2.00"), p3.getAmount());
        assertEquals("428400112233", p3.getUtr());
    }
}
