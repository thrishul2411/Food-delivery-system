package com.food.delivery.paymentservice.service;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.entity.PaymentTransaction;
// *** IMPORT the Event DTO ***
import com.food.delivery.paymentservice.event.PaymentOutcomeEvent;
import com.food.delivery.paymentservice.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
// *** IMPORT StreamBridge ***
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    // *** Autowire StreamBridge ***
    @Autowired
    private StreamBridge streamBridge;

    // *** Define the Binding Name (must match properties) ***
    // This is the logical name of the output channel configured in application properties.
    private static final String PAYMENT_OUTCOME_BINDING_NAME = "paymentOutcomeSupplier-out-0";


    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO request) {
        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(txn -> {
            throw new IllegalStateException("Payment already initiated for order: " + request.getOrderId());
        });

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING") // Initial state remains PENDING
                .provider("MOCK")
                .providerTransactionId(UUID.randomUUID().toString())
                .build();
        PaymentTransaction savedTransaction = paymentRepository.save(transaction);

        // Update the response message to guide the user for the next step
        return PaymentResponseDTO.builder()
                .transactionId(savedTransaction.getId())
                .orderId(savedTransaction.getOrderId())
                .status(savedTransaction.getStatus())
                .amount(savedTransaction.getAmount())
                .currency(savedTransaction.getCurrency())
                .message("Payment initiated. Use PUT /api/payments/confirm/" + savedTransaction.getId() + "?success=true (or false) to simulate completion.") // More specific message
                .paymentGatewayUrl("http://mock-payment-gateway.com/pay?txn=" + savedTransaction.getProviderTransactionId())
                .build();
    }

    // *** NEW METHOD to confirm payment and PUBLISH event ***
    @Transactional
    public PaymentOutcomeEvent confirmPayment(Long transactionId, boolean success) {
        // Find the existing transaction
        PaymentTransaction txn = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Ensure it's in a state that can be confirmed
        if (!"PENDING".equals(txn.getStatus())) {
            throw new IllegalStateException("Payment is not in PENDING state. Current status: " + txn.getStatus());
        }

        // Update the status based on the 'success' flag
        String newStatus = success ? "SUCCESSFUL" : "FAILED";
        txn.setStatus(newStatus);
        paymentRepository.save(txn); // Save updated status to DB

        // Create the event payload using the builder
        PaymentOutcomeEvent event = PaymentOutcomeEvent.builder()
                .orderId(txn.getOrderId())
                .transactionId(txn.getId())
                .status(newStatus)
                .amount(txn.getAmount()) // Include amount for context
                .failureReason(success ? null : "Mock payment failure simulation") // Add reason on failure
                .build();

        // Publish the event to the configured binding using StreamBridge
        System.out.println("PAYMENT-SERVICE: Publishing event to binding [" + PAYMENT_OUTCOME_BINDING_NAME + "]: " + event); // Log before sending
        boolean sent = streamBridge.send(PAYMENT_OUTCOME_BINDING_NAME, event);
        System.out.println("PAYMENT-SERVICE: Event sent successfully? " + sent); // Log outcome

        return event; // Return the published event details
    }
}