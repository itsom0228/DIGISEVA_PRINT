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
                    "Laptop Demonstration Kiosk Display",
                    "Interactive Screen Terminal (Laptop Display)",
                    "Pune",
                    "Maharashtra",
                    KioskStatus.ACTIVE
            ));
        }

        if (kioskRepository.count() <= 1) {
            kioskRepository.save(new Kiosk(
                    "TAHSIL001",
                    "Tahsil Office Demonstration Kiosk",
                    "Tahsil Office, Haveli Sub-Division, Pune",
                    "Pune",
                    "Maharashtra",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "COLLEGE001",
                    "Government Engineering College Kiosk",
                    "Shivajinagar Campus, Pune",
                    "Pune",
                    "Maharashtra",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "HOSPITAL001",
                    "District Civil Hospital Kiosk",
                    "Civil Hospital Complex, Satara",
                    "Satara",
                    "Maharashtra",
                    KioskStatus.ACTIVE
            ));

            kioskRepository.save(new Kiosk(
                    "LIBRARY001",
                    "Central Public Library Kiosk",
                    "Budhwar Peth, Pune",
                    "Pune",
                    "Maharashtra",
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
                .orElseThrow(() -> new ResourceNotFoundException("Kiosk not found with ID: " + kioskId));
    }

    public boolean isValidKiosk(String kioskId) {
        return kioskRepository.existsByKioskId(kioskId);
    }
}
