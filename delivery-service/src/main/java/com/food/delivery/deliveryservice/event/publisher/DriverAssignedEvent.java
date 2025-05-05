package com.food.delivery.deliveryservice.event.publisher;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverAssignedEvent {
    private Long orderId;
    private Long driverId;
}