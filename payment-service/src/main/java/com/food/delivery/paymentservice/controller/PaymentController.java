package com.food.delivery.paymentservice.controller;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.service.PaymentService;

import com.food.delivery.paymentservice.event.PaymentOutcomeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error initiating payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error initiating payment.");
        }
    }

    @PutMapping("/confirm/{transactionId}")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long transactionId,
            @RequestParam(defaultValue = "true") boolean success) {
        try {

            PaymentOutcomeEvent event = paymentService.confirmPayment(transactionId, success);

            return ResponseEntity.ok("Payment status updated to " + event.getStatus() + " for transaction " + transactionId + " and event published.");
        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {

            System.err.println("Error confirming payment for transaction " + transactionId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error confirming payment.");
        }
    }

    // TODO: Add POST endpoint for webhook later
}