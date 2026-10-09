package com.printwithai.service;

import com.printwithai.entity.Kiosk;
import com.printwithai.enums.KioskStatus;
import com.printwithai.exception.ResourceNotFoundException;
import com.printwithai.repository.KioskRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KioskService {

    private final KioskRepository kioskRepository;

    public KioskService(KioskRepository kioskRepository) {
        this.kioskRepository = kioskRepository;
    }

    private final java.util.concurrent.ConcurrentMap<String, java.time.Instant> lastCustomerConnections = new java.util.concurrent.ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialKiosks() {
        if (!kioskRepository.existsByKioskId("LAPTOP001")) {
            kioskRepository.save(new Kiosk(
                    "LAPTOP001",
                    "DigiSeva Laptop Display Terminal",
                    "Citizen Self-Service Screen Terminal",
                    "Pune",
                    "Maharashtra",
                    "tahsil-office",
                    KioskStatus.ACTIVE
            ));
        }

        if (kioskRepository.count() <= 1) {
            kioskRepository.save(new Kiosk(
                    "TAHSIL001",
                    "Tahsil & Revenue Office Terminal",
                    "Tahsil Office, Haveli Sub-Division, Pune",
                    "Pune",
                    "Maharashtra",
                    "tahsil-office",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "COLLEGE001",
                    "Government College Main Terminal",
                    "Administrative Wing, COEP Campus, Pune",
                    "Pune",
                    "Maharashtra",
                    "college-university",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "HOSPITAL001",
                    "District Civil Hospital Terminal",
                    "OPD Registration Hall, Civil Hospital, Satara",
                    "Satara",
                    "Maharashtra",
                    "hospital",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "GRAM001",
                    "Gram Panchayat Suvidha Terminal",
                    "Gram Panchayat Office, Wagholi, Pune",
                    "Pune",
                    "Maharashtra",
                    "gram-panchayat",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "MUNICIPAL001",
                    "Municipal Corporation Terminal",
                    "PMC Citizen Facilitation Center, Shivajinagar",
                    "Pune",
                    "Maharashtra",
                    "municipal-corporation",
                    KioskStatus.ACTIVE
            ));
        }
    }

    public void recordCustomerConnection(String kioskId) {
        if (kioskId != null) {
            lastCustomerConnections.put(kioskId, java.time.Instant.now());
        }
    }

    public java.time.Instant getLastCustomerConnection(String kioskId) {
        return kioskId != null ? lastCustomerConnections.get(kioskId) : null;
    }

    public void clearCustomerConnection(String kioskId) {
        if (kioskId != null) {
            lastCustomerConnections.remove(kioskId);
        }
    }

    public List<Kiosk> getAllKiosks() {
        return kioskRepository.findAll();
    }

    public Kiosk getKioskById(String kioskId) {
        return kioskRepository.findByKioskId(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine terminal not found with ID: " + kioskId));
    }

    public Kiosk getKioskByEntityId(Long id) {
        return kioskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with database ID: " + id));
    }

    public boolean isValidKiosk(String kioskId) {
        return kioskRepository.existsByKioskId(kioskId);
    }

    public Kiosk getKioskForOfficeCategory(String categorySlug) {
        if (categorySlug != null && !categorySlug.trim().isEmpty()) {
            return kioskRepository.findFirstByOfficeCategorySlugAndStatus(categorySlug.trim(), KioskStatus.ACTIVE)
                    .orElseGet(() -> kioskRepository.findByOfficeCategorySlug(categorySlug.trim()).stream().findFirst()
                            .orElse(getKioskById("TAHSIL001")));
        }
        return getKioskById("TAHSIL001");
    }

    @Transactional
    public Kiosk saveKiosk(Kiosk kiosk) {
        if (kiosk.getId() == null) {
            if (kioskRepository.existsByKioskId(kiosk.getKioskId())) {
                throw new IllegalArgumentException("A machine with ID '" + kiosk.getKioskId() + "' already exists.");
            }
            return kioskRepository.save(kiosk);
        } else {
            Kiosk existing = getKioskByEntityId(kiosk.getId());
            existing.setName(kiosk.getName());
            existing.setLocation(kiosk.getLocation());
            existing.setCity(kiosk.getCity());
            existing.setState(kiosk.getState());
            existing.setStatus(kiosk.getStatus());
            existing.setOfficeCategorySlug(kiosk.getOfficeCategorySlug());
            return kioskRepository.save(existing);
        }
    }

    @Transactional
    public void toggleKioskStatus(Long id) {
        Kiosk existing = getKioskByEntityId(id);
        if (existing.getStatus() == KioskStatus.ACTIVE) {
            existing.setStatus(KioskStatus.MAINTENANCE);
        } else {
            existing.setStatus(KioskStatus.ACTIVE);
        }
        kioskRepository.save(existing);
    }

    @Transactional
    public void deleteKiosk(Long id) {
        Kiosk existing = getKioskByEntityId(id);
        kioskRepository.delete(existing);
    }
}
