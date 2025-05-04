package com.food.delivery.paymentservice.service;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.entity.PaymentTransaction;
import com.food.delivery.paymentservice.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO request) {
        // Basic validation: Check if payment already exists for order?
        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(txn -> {
            throw new IllegalStateException("Payment already initiated for order: " + request.getOrderId());
        });

        // Create transaction record in PENDING state
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING") // Initial state
                .provider("MOCK") // Using mock provider for now
                .providerTransactionId(UUID.randomUUID().toString()) // Simulate a provider ID
                .build();

        PaymentTransaction savedTransaction = paymentRepository.save(transaction);

        // In a real scenario:
        // 1. Call external payment gateway API here with amount, currency, order details, callback URL.
        // 2. The gateway might return a redirect URL for the user or require further steps.
        // 3. Store the provider's transaction ID.

        // Simulate initiation success
        return PaymentResponseDTO.builder()
                .transactionId(savedTransaction.getId())
                .orderId(savedTransaction.getOrderId())
                .status(savedTransaction.getStatus())
                .amount(savedTransaction.getAmount())
                .currency(savedTransaction.getCurrency())
                .message("Payment initiated. Mock provider ID: " + savedTransaction.getProviderTransactionId())
                .paymentGatewayUrl("http://mock-payment-gateway.com/pay?txn=" + savedTransaction.getProviderTransactionId()) // Dummy URL
                .build();
    }

    // TODO: Add methods to handle webhook callbacks from payment gateway
    // This method would update the status (SUCCESSFUL/FAILED) and publish events later
    // public void processPaymentWebhook(String providerTransactionId, String status) { ... }
}