package com.food.delivery.orderservice.event; // Make sure package exists

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutcomeEvent {
    private Long orderId;
    private String status; // "SUCCESSFUL" or "FAILED"
    private Long transactionId;
    private BigDecimal amount;
    private String failureReason;
}