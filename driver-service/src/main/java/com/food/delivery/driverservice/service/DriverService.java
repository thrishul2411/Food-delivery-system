package com.food.delivery.driverservice.service;

import com.food.delivery.driverservice.entity.DriverProfile;
import com.food.delivery.driverservice.event.DriverLocationUpdatedEvent;
import com.food.delivery.driverservice.repository.DriverProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private StreamBridge streamBridge;

    private static final String DRIVER_LOCATION_BINDING_NAME = "driverLocationSupplier-out-0";

    @Transactional
    public void updateAvailability(Long driverId, boolean available) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver not found with ID: " + driverId));

        profile.setAvailable(available);
        profile.setAvailabilityLastUpdated(LocalDateTime.now());
        driverProfileRepository.save(profile);
        System.out.println("DRIVER-SERVICE: Updated availability for driver " + driverId + " to " + available);
        // TODO: Optionally publish DriverAvailabilityChangedEvent if needed by other services
    }

    @Transactional
    public void updateLocation(Long driverId, Double latitude, Double longitude) {

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and Longitude cannot be null.");
        }


        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver not found with ID: " + driverId));

        LocalDateTime now = LocalDateTime.now();
        profile.setCurrentLatitude(latitude);
        profile.setCurrentLongitude(longitude);
        profile.setLastLocationUpdate(now);
        driverProfileRepository.save(profile);
        System.out.println("DRIVER-SERVICE: Updated location for driver " + driverId + " to [" + latitude + "," + longitude + "]");

        DriverLocationUpdatedEvent event = DriverLocationUpdatedEvent.builder()
                .driverId(driverId)
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(now)
                .build();

        System.out.println("DRIVER-SERVICE: Publishing event to binding [" + DRIVER_LOCATION_BINDING_NAME + "]: " + event);
        streamBridge.send(DRIVER_LOCATION_BINDING_NAME, event);
    }

    public void createDriverProfile(Long driverId, String vehicleDetails) {
        if(driverProfileRepository.existsById(driverId)) {
            System.out.println("DRIVER-SERVICE: Profile for driver " + driverId + " already exists.");
            return;
        }
        DriverProfile profile = DriverProfile.builder()
                .driverId(driverId)
                .vehicleDetails(vehicleDetails)
                .available(false)
                .build();
        driverProfileRepository.save(profile);
        System.out.println("DRIVER-SERVICE: Created profile for driver " + driverId);
    }

    public List<DriverProfile> findAvailableDrivers() {

        System.out.println("DRIVER-SERVICE: Finding available drivers...");
        List<DriverProfile> available = driverProfileRepository.findByAvailableTrue();
        System.out.println("DRIVER-SERVICE: Found " + available.size() + " available drivers.");
        return available;
    }
}