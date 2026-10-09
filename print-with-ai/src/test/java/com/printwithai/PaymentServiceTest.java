package com.printwithai;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.exception.InvalidOrderStateException;
import com.printwithai.printer.PrinterService;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PaymentTransactionRepository;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.service.impl.DemoPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private DemoPaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new DemoPaymentServiceImpl(
                printJobRepository,
                paymentTransactionRepository,
                printerService,
                kioskLogRepository
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
}
