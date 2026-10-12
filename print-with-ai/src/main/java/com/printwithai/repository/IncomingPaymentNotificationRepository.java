package com.printwithai.repository;

import com.printwithai.entity.IncomingPaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomingPaymentNotificationRepository extends JpaRepository<IncomingPaymentNotification, Long> {
    Optional<IncomingPaymentNotification> findFirstByUtrOrderByCreatedAtDesc(String utr);
    List<IncomingPaymentNotification> findByUtrOrderByCreatedAtDesc(String utr);
    List<IncomingPaymentNotification> findTop20ByOrderByCreatedAtDesc();
}
