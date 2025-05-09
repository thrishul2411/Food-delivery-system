package com.food.delivery.orderservice.event.consumer;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPickedUpEvent {
    private Long orderId;
    private Long driverId;
    private LocalDateTime timestamp;
}