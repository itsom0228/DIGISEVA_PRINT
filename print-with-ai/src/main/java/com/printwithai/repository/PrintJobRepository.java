package com.printwithai.repository;

import com.printwithai.entity.PrintJob;
import com.printwithai.enums.PaymentStatus;
import com.printwithai.enums.PrintJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {

    Optional<PrintJob> findByPublicReference(String publicReference);

    Optional<PrintJob> findByTrackingToken(String trackingToken);

    List<PrintJob> findByKioskIdOrderByCreatedAtDesc(String kioskId);

    Optional<PrintJob> findFirstByKioskIdOrderByCreatedAtDesc(String kioskId);

    long countByJobStatus(PrintJobStatus jobStatus);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM PrintJob p WHERE p.paymentStatus = 'DEMO_SUCCESS'")
    BigDecimal calculateTotalDemoSales();

    @Query("SELECT p FROM PrintJob p WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.publicReference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.kioskId) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:jobStatus IS NULL OR p.jobStatus = :jobStatus) AND " +
           "(:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus) " +
           "ORDER BY p.createdAt DESC")
    Page<PrintJob> searchJobs(
        @Param("query") String query,
        @Param("jobStatus") PrintJobStatus jobStatus,
        @Param("paymentStatus") PaymentStatus paymentStatus,
        Pageable pageable
    );

    List<PrintJob> findByCreatedAtBeforeAndFileDeletedFalse(LocalDateTime cutoff);

    List<PrintJob> findTop10ByOrderByCreatedAtDesc();
}
