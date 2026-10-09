package com.printwithai.entity;

import com.printwithai.enums.KioskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kiosks", indexes = {
    @Index(name = "idx_kiosk_id", columnList = "kiosk_identifier", unique = true)
})
public class Kiosk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kiosk_identifier", nullable = false, unique = true, length = 50)
    private String kioskId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KioskStatus status = KioskStatus.ACTIVE;

    @Column(name = "office_category_slug", length = 80)
    private String officeCategorySlug = "tahsil-office";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Kiosk() {}

    public Kiosk(String kioskId, String name, String location, String city, String state, KioskStatus status) {
        this(kioskId, name, location, city, state, "tahsil-office", status);
    }

    public Kiosk(String kioskId, String name, String location, String city, String state, String officeCategorySlug, KioskStatus status) {
        this.kioskId = kioskId;
        this.name = name;
        this.location = location;
        this.city = city;
        this.state = state;
        this.officeCategorySlug = (officeCategorySlug != null && !officeCategorySlug.trim().isEmpty()) ? officeCategorySlug.trim() : "tahsil-office";
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKioskId() { return kioskId; }
    public void setKioskId(String kioskId) { this.kioskId = kioskId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public KioskStatus getStatus() { return status; }
    public void setStatus(KioskStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOfficeCategorySlug() { return officeCategorySlug; }
    public void setOfficeCategorySlug(String officeCategorySlug) { this.officeCategorySlug = officeCategorySlug; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
