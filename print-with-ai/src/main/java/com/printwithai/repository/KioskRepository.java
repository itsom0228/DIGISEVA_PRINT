package com.printwithai.repository;

import com.printwithai.entity.Kiosk;
import com.printwithai.enums.KioskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KioskRepository extends JpaRepository<Kiosk, Long> {
    Optional<Kiosk> findByKioskId(String kioskId);
    boolean existsByKioskId(String kioskId);
    List<Kiosk> findByOfficeCategorySlug(String officeCategorySlug);
    Optional<Kiosk> findFirstByOfficeCategorySlugAndStatus(String officeCategorySlug, KioskStatus status);
}

