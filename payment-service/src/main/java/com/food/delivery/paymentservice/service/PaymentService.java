package com.food.delivery.paymentservice.service;

import com.food.delivery.paymentservice.dto.PaymentRequestDTO;
import com.food.delivery.paymentservice.dto.PaymentResponseDTO;
import com.food.delivery.paymentservice.entity.PaymentTransaction;

import com.food.delivery.paymentservice.event.PaymentOutcomeEvent;
import com.food.delivery.paymentservice.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    @Autowired
    private StreamBridge streamBridge;

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

    @Transactional
    public PaymentOutcomeEvent confirmPayment(Long transactionId, boolean success) {

        PaymentTransaction txn = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!"PENDING".equals(txn.getStatus())) {
            throw new IllegalStateException("Payment is not in PENDING state. Current status: " + txn.getStatus());
        }

        String newStatus = success ? "SUCCESSFUL" : "FAILED";
        txn.setStatus(newStatus);
        paymentRepository.save(txn);

        PaymentOutcomeEvent event = PaymentOutcomeEvent.builder()
                .orderId(txn.getOrderId())
                .transactionId(txn.getId())
                .status(newStatus)
                .amount(txn.getAmount())
                .failureReason(success ? null : "Mock payment failure simulation")
                .build();

        System.out.println("PAYMENT-SERVICE: Publishing event to binding [" + PAYMENT_OUTCOME_BINDING_NAME + "]: " + event);
        boolean sent = streamBridge.send(PAYMENT_OUTCOME_BINDING_NAME, event);
        System.out.println("PAYMENT-SERVICE: Event sent successfully? " + sent);

        return event;
    }
}