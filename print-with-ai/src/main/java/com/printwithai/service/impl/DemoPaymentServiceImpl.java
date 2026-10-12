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

import java.math.BigDecimal;
import java.util.Optional;

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
    public PaymentTransaction submitPaymentVerification(String trackingToken, String utrNumber) {
        PrintJob job = printJobRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found for token: " + trackingToken));

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("This order has already been paid and verified.");
        }

        if (job.getJobStatus() == PrintJobStatus.CANCELLED || job.getJobStatus() == PrintJobStatus.FAILED) {
            throw new InvalidOrderStateException("Cannot submit payment for a " + job.getJobStatus() + " print order.");
        }

        if (utrNumber == null || utrNumber.trim().length() < 6) {
            throw new IllegalArgumentException("कृपया वैध १२ अंकी UPI ट्रान्झॅक्शन आयडी (UTR) प्रविष्ट करा.");
        }

        String cleanUtr = utrNumber.trim().toUpperCase();

        // Check if this exact UTR was already successfully used for another job
        Optional<PaymentTransaction> existingTxn = paymentTransactionRepository.findByTransactionReference(cleanUtr);
        if (existingTxn.isPresent()) {
            PaymentTransaction prev = existingTxn.get();
            if (prev.getPrintJob() != null && !prev.getPrintJob().getId().equals(job.getId())
                    && prev.getStatus() == PaymentStatus.DEMO_SUCCESS) {
                throw new IllegalArgumentException("हा UPI UTR (" + cleanUtr + ") आधीच दुसऱ्या ऑर्डरसाठी वापरला गेला आहे! कृपया नवीन ट्रान्झॅक्शन आयडी टाका.");
            }
        }

        // Find existing transaction for this job or create a new pending one
        PaymentTransaction transaction = paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)
                .orElseGet(() -> new PaymentTransaction(
                        job,
                        cleanUtr,
                        PaymentMode.DEMO_UPI,
                        job.getTotalAmount(),
                        PaymentStatus.DEMO_PENDING,
                        "UTR Submitted by customer: " + cleanUtr
                ));

        transaction.setTransactionReference(cleanUtr);
        transaction.setStatus(PaymentStatus.DEMO_PENDING);
        transaction.setAmount(job.getTotalAmount());
        transaction.setNotes("UTR Submitted: " + cleanUtr + ". Awaiting operator/machine verification.");

        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        // Update Job Status
        job.setPaymentStatus(PaymentStatus.DEMO_PENDING);
        job.setFailureReason(null);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "PAYMENT_UTR_SUBMITTED",
                String.format("Customer submitted UPI UTR: %s for ₹%s. Awaiting verification.",
                        cleanUtr, job.getTotalAmount().toPlainString())
        ));

        log.info("[PAYMENT UTR SUBMITTED] Job {}: UTR {} recorded. Awaiting cross-verification before printing.",
                job.getPublicReference(), cleanUtr);

        // NOTICE: printerService.submitJob(job) is INTENTIONALLY NOT CALLED HERE.
        // Printing only occurs once payment is genuinely cross-verified!
        return savedTxn;
    }

    @Override
    @Transactional
    public PaymentTransaction verifyAndApprovePayment(String trackingToken, BigDecimal receivedAmount, String approvedBy) {
        PrintJob job = printJobRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found for token: " + trackingToken));

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("This order has already been paid and verified.");
        }

        if (job.getJobStatus() == PrintJobStatus.CANCELLED) {
            throw new InvalidOrderStateException("Cannot verify payment for a cancelled print order.");
        }

        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("प्राप्त रक्कम शून्य किंवा अवैध असू शकत नाही.");
        }

        BigDecimal requiredAmount = job.getTotalAmount();

        // STRICT CROSS-VERIFICATION: If user sent ₹1 when ₹2 is required, REJECT AND BLOCK PRINT
        if (receivedAmount.compareTo(requiredAmount) < 0) {
            String mismatchReason = String.format("अपूर्ण रक्कम! देय रक्कम ₹%s आहे, परंतु केवळ ₹%s प्राप्त झाले. (फरक: ₹%s)",
                    requiredAmount.toPlainString(), receivedAmount.toPlainString(),
                    requiredAmount.subtract(receivedAmount).toPlainString());
            rejectPayment(trackingToken, mismatchReason);
            throw new IllegalArgumentException(mismatchReason);
        }

        // Find or create transaction
        PaymentTransaction transaction = paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job)
                .orElseGet(() -> new PaymentTransaction(
                        job,
                        ReferenceGenerator.generateTransactionReference(),
                        PaymentMode.DEMO_UPI,
                        receivedAmount,
                        PaymentStatus.DEMO_SUCCESS,
                        "Verified by " + approvedBy
                ));

        transaction.setStatus(PaymentStatus.DEMO_SUCCESS);
        transaction.setAmount(receivedAmount);
        transaction.setNotes("Payment Verified: ₹" + receivedAmount.toPlainString() + " received. Approved by: " + approvedBy);
        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        // Update Job Status
        job.setPaymentStatus(PaymentStatus.DEMO_SUCCESS);
        job.setFailureReason(null);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "PAYMENT_SUCCESS",
                String.format("Payment of ₹%s verified and confirmed by %s (Txn Ref: %s).",
                        receivedAmount.toPlainString(), approvedBy, savedTxn.getTransactionReference())
        ));

        log.info("[PAYMENT VERIFIED] Payment of ₹{} confirmed for job {}. Submitting to hardware printer queue.",
                receivedAmount.toPlainString(), job.getPublicReference());

        // STRICT GATEWAY: Immediately release and dispatch to printer queue
        printerService.submitJob(job);

        return savedTxn;
    }

    @Override
    @Transactional
    public void rejectPayment(String trackingToken, String reason) {
        PrintJob job = printJobRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Print order not found for token: " + trackingToken));

        if (job.getPaymentStatus() == PaymentStatus.DEMO_SUCCESS) {
            throw new InvalidOrderStateException("Cannot reject an already confirmed and printed order.");
        }

        paymentTransactionRepository.findFirstByPrintJobOrderByCreatedAtDesc(job).ifPresent(txn -> {
            txn.setStatus(PaymentStatus.DEMO_FAILED);
            txn.setNotes("Payment Rejected: " + reason);
            paymentTransactionRepository.save(txn);
        });

        job.setPaymentStatus(PaymentStatus.DEMO_FAILED);
        job.setFailureReason(reason);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "PAYMENT_REJECTED",
                "Payment rejected for order " + job.getPublicReference() + ": " + reason
        ));

        log.warn("[PAYMENT REJECTED] Order {}: {}. Printing aborted.", job.getPublicReference(), reason);
        // NO PRINTER DISPATCH!
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

        String txnRef = (simulatedUpiId != null && !simulatedUpiId.isBlank())
                ? simulatedUpiId.trim().toUpperCase()
                : ReferenceGenerator.generateTransactionReference();

        PaymentTransaction transaction = new PaymentTransaction(
                job,
                txnRef,
                paymentMode,
                job.getTotalAmount(),
                PaymentStatus.DEMO_SUCCESS,
                "Simulated Payment (" + paymentMode.name() + ")"
        );

        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        job.setPaymentStatus(PaymentStatus.DEMO_SUCCESS);
        job.setFailureReason(null);
        printJobRepository.save(job);

        kioskLogRepository.save(new KioskLog(
                job.getKioskId(),
                job.getPublicReference(),
                "PAYMENT_SUCCESS",
                String.format("Payment of ₹%s received via %s. Ref: %s",
                        job.getTotalAmount().toPlainString(), paymentMode.name(), txnRef)
        ));

        log.info("[DEMO PAYMENT] Payment confirmed for job {}. Submitting to printer queue.", job.getPublicReference());

        // Immediately submit to printer queue
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
