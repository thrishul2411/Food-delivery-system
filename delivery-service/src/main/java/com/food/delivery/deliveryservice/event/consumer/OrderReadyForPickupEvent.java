package com.food.delivery.deliveryservice.event.consumer;
import lombok.Data;
// Define the fields needed by delivery service
@Data
public class OrderReadyForPickupEvent {
    private Long orderId;
    private Long restaurantId;
    // Add restaurant location (lat/lon) if needed for driver selection
    // private double restaurantLat;
    // private double restaurantLon;
}