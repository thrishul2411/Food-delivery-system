package com.food.delivery.driverservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {


    @Id
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "vehicle_details")
    private String vehicleDetails;

    private Double currentLatitude;
    private Double currentLongitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    @Column(name = "is_available", nullable = false)
    private boolean available = false;

    @Column(name = "availability_last_updated")
    private LocalDateTime availabilityLastUpdated;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        availabilityLastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}