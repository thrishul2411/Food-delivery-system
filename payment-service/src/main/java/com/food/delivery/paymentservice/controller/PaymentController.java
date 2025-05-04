package com.food.delivery.paymentservice.controller;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.service.PaymentService;
// *** IMPORT Event DTO ***
import com.food.delivery.paymentservice.event.PaymentOutcomeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Ensure @RestController etc. are imported

// *** IMPORT required annotations for the new endpoint ***
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDTO request) {
        try {
            PaymentResponseDTO response = paymentService.initiatePayment(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict is appropriate
        } catch (Exception e) {
            System.err.println("Error initiating payment: " + e.getMessage()); // Log unexpected errors
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error initiating payment.");
        }
    }

    // *** NEW Endpoint to simulate payment confirmation ***
    // Maps to PUT requests like /api/payments/confirm/123?success=true
    @PutMapping("/confirm/{transactionId}")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long transactionId,
            @RequestParam(defaultValue = "true") boolean success) { // Use query param to indicate success/failure
        try {
            // Call the service method that updates status and publishes the event
            PaymentOutcomeEvent event = paymentService.confirmPayment(transactionId, success);
            // Return a simple success message indicating the outcome
            return ResponseEntity.ok("Payment status updated to " + event.getStatus() + " for transaction " + transactionId + " and event published.");
        } catch (IllegalArgumentException e) {
            // Transaction not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404 Not Found is appropriate
        } catch (IllegalStateException e) {
            // Payment not in PENDING state
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 400 Bad Request
        } catch (Exception e) {
            // Catch other potential errors during confirmation/publishing
            System.err.println("Error confirming payment for transaction " + transactionId + ": " + e.getMessage()); // Log unexpected errors
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error confirming payment.");
        }
    }

    // TODO: Add POST endpoint for webhook later
}