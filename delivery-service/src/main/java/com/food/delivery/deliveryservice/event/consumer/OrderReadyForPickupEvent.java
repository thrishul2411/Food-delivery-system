package com.food.delivery.deliveryservice.event.consumer;
import lombok.Data;

@Data
public class OrderReadyForPickupEvent {
    private Long orderId;
    private Long restaurantId;

}