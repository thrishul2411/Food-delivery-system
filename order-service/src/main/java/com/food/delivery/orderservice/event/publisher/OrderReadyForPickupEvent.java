package com.food.delivery.orderservice.event.publisher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderReadyForPickupEvent {
    private Long orderId;
    private Long restaurantId;
}