package com.food.delivery.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data // Lombok: Generates getters, setters, toString, etc.
@Builder // Lombok: Provides the builder pattern for easy object creation
@NoArgsConstructor // Lombok: Needed for frameworks like Jackson (JSON mapping)
@AllArgsConstructor // Lombok: Constructor with all arguments
public class PaymentOutcomeEvent {
    private Long orderId;
    private String status; // e.g., "SUCCESSFUL" or "FAILED"
    private Long transactionId;
    private BigDecimal amount; // Optional: Amount for reference/auditing
    private String failureReason; // Optional: Include if status is FAILED
}