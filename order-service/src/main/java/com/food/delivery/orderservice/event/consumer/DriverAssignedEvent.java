package com.food.delivery.orderservice.event.consumer;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverAssignedEvent {
    private Long orderId;
    private Long driverId;
}