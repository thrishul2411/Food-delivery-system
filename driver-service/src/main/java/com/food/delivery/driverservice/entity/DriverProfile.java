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

    // Use the same ID as the User service for consistency
    // We are NOT generating this ID here, it comes from the User service concept
    @Id
    @Column(name = "driver_id")
    private Long driverId; // Corresponds to user_id in user_service

    @Column(name = "vehicle_details")
    private String vehicleDetails; // e.g., "Honda Civic - ABC 123"

    // Current location - updated frequently
    private Double currentLatitude;
    private Double currentLongitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    // Current availability status
    @Column(name = "is_available", nullable = false)
    private boolean available = false; // Default to offline

    @Column(name = "availability_last_updated")
    private LocalDateTime availabilityLastUpdated;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // We might need a way to initially create this profile
    // potentially triggered by an event when a user gets a DRIVER role
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