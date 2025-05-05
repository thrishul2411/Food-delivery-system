package com.food.delivery.driverservice.repository;

import com.food.delivery.driverservice.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> { // ID is Long (driverId)

    // Find available drivers (useful for delivery service later)
    List<DriverProfile> findByAvailableTrue();

    // Add methods to f ind drivers within a certain geographical area later
}