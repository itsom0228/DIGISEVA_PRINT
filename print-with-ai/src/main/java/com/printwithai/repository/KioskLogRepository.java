package com.printwithai.repository;

import com.printwithai.entity.KioskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KioskLogRepository extends JpaRepository<KioskLog, Long> {
    List<KioskLog> findTop20ByOrderByCreatedAtDesc();
    List<KioskLog> findByKioskIdOrderByCreatedAtDesc(String kioskId);
    List<KioskLog> findByOrderReferenceOrderByCreatedAtDesc(String orderReference);
}
