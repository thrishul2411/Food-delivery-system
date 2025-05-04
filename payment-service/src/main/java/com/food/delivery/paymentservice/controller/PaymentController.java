package com.food.delivery.paymentservice.controller;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDTO request) {
        try {
            PaymentResponseDTO response = paymentService.initiatePayment(request);
            // Return 200 OK with details including mock gateway URL
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Handle cases like payment already initiated
            return ResponseEntity.status(409).body(e.getMessage()); // 409 Conflict
        } catch (Exception e) {
            // Handle generic errors
            return ResponseEntity.status(500).body("Error processing payment initiation: " + e.getMessage());
        }
    }

    // TODO: Add a POST endpoint for '/webhook/payment_status' later
    // This would receive callbacks from the external payment gateway
    // Needs careful security (signature verification)
    // @PostMapping("/webhook/payment_status")
    // public ResponseEntity<Void> handleWebhook(...) { ... }
}