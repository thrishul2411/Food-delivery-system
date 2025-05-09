package com.food.delivery.deliveryservice.controller;

import com.food.delivery.deliveryservice.service.DeliveryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Ensure correct imports

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<?> updateDeliveryStatus(
            @PathVariable Long assignmentId,
            @RequestBody DeliveryStatusUpdateRequest request) {
        try {
            deliveryService.updateDeliveryStatus(assignmentId, request.getStatus());
            return ResponseEntity.ok("Status updated successfully.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error updating delivery status for assignment " + assignmentId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error updating status.");
        }
    }


}