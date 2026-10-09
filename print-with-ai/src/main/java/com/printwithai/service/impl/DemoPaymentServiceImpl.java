package com.printwithai.service.impl;

import com.printwithai.entity.KioskLog;
import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentMode;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import com.printwithai.exception.InvalidOrderStateException;
import com.printwithai.exception.ResourceNotFoundException;
import com.printwithai.printer.PrinterService;
import com.printwithai.repository.KioskLogRepository;
import com.printwithai.repository.PaymentTransactionRepository;
import com.printwithai.repository.PrintJobRepository;
import com.printwithai.service.PaymentService;
import com.printwithai.util.ReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoPaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(DemoPaymentServiceImpl.class);

    private final PrintJobRepository printJobRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PrinterService printerService;
    private final KioskLogRepository kioskLogRepository;

    public DemoPaymentServiceImpl(
            PrintJobRepository printJobRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PrinterService printerService,
            KioskLogRepository kioskLogRepository) {
        this.printJobRepository = printJobRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.printerService = printerService;
        this.kioskLogRepository = kioskLogRepository;
    }

    @Override
    @Transactional
    public PaymentTransaction processDemoPayment(String trackingToken, PaymentMode paymentMode, String simulatedUpiId) {
        PrintJob job = printJobRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found for token: " + trackingToken));

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("This order has already been paid successfully.");
        }

        if (job.getJobStatus() == PrintJobStatus.CANCELLED || job.getJobStatus() == PrintJobStatus.FAILED) {
            throw new InvalidOrderStateException("Cannot pay for a " + job.getJobStatus() + " print order.");
        }

        String txnRef = ReferenceGenerator.generateTransactionReference();
        String note = (paymentMode == PaymentMode.DEMO_UPI)
                ? "Simulated UPI Payment (ID: " + (simulatedUpiId != null && !simulatedUpiId.isBlank() ? simulatedUpiId : "demo@upi") + ")"
                : "Simulated Counter Cash Settlement";

        PaymentTransaction transaction = new PaymentTransaction(
                job,
                txnRef,
                paymentMode,
                job.getTotalAmount(),
                PaymentStatus.DEMO_SUCCESS,
                note
        );

        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        // Update Job Status
        job.setPaymentStatus(PaymentStatus.DEMO_SUCCESS);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "PAYMENT_SUCCESS",
                String.format("Demo payment of ₹%s received via %s. Ref: %s",
                        job.getTotalAmount().toPlainString(), paymentMode.name(), txnRef)
        ));

        log.info("[DEMO PAYMENT] Payment confirmed for job {}. Submitting to printer queue.", job.getPublicReference());

        // Immediately submit to mock printer queue
        printerService.submitJob(job);

        return savedTxn;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getTransaction(String transactionReference) {
        return paymentTransactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found: " + transactionReference));
    }
}
