package com.printwithai.repository;

import com.printwithai.entity.PaymentTransaction;
import com.printwithai.entity.PrintJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionReference(String transactionReference);
    List<PaymentTransaction> findByPrintJobOrderByCreatedAtDesc(PrintJob printJob);
}
