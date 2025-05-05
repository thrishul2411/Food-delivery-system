package com.food.delivery.driverservice.controller;

import com.food.delivery.driverservice.dto.AvailabilityRequest;
import com.food.delivery.driverservice.dto.LocationRequest;
import com.food.delivery.driverservice.entity.DriverProfile;
import com.food.delivery.driverservice.service.DriverService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    // PUT /api/drivers/{driverId}/availability
    @PutMapping("/{driverId}/availability")
    public ResponseEntity<?> updateAvailability(@PathVariable Long driverId, @RequestBody AvailabilityRequest request) {
        try {
            driverService.updateAvailability(driverId, request.isAvailable());
            return ResponseEntity.noContent().build(); // HTTP 204 No Content on success
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // HTTP 404 Not Found
        } catch (Exception e) {
            System.err.println("Error updating availability for driver " + driverId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error updating availability.");
        }
    }

    // PUT /api/drivers/{driverId}/location
    @PutMapping("/{driverId}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long driverId, @RequestBody LocationRequest request) {
        try {
            driverService.updateLocation(driverId, request.getLatitude(), request.getLongitude());
            return ResponseEntity.noContent().build(); // HTTP 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // HTTP 404
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // HTTP 400 for bad coords
        } catch (Exception e) {
            System.err.println("Error updating location for driver " + driverId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error updating location.");
        }
    }

    // POST /api/drivers/dummy/{driverId} - Helper to create a profile for testing
    @PostMapping("/dummy/{driverId}")
    public ResponseEntity<?> createDummyProfile(@PathVariable Long driverId, @RequestParam(defaultValue = "Test Car - XYZ 789") String vehicle) {
        try {
            driverService.createDriverProfile(driverId, vehicle);
            return ResponseEntity.status(HttpStatus.CREATED).body("Profile created for driver " + driverId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating dummy profile: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<DriverProfile>> getDrivers(
            @RequestParam(name = "available", required = false) Boolean available) {

        if (Boolean.TRUE.equals(available)) { // Check if parameter is present and true
            List<DriverProfile> drivers = driverService.findAvailableDrivers();
            return ResponseEntity.ok(drivers);
        } else {
            // Optional: Implement fetching all drivers or return error/empty list
            // For now, only support fetching available=true
            return ResponseEntity.badRequest().body(null); // Or return empty list: ResponseEntity.ok(Collections.emptyList());
        }
    }
}