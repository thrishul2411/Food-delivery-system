package com.food.delivery.deliveryservice.client;
import lombok.Data;
@Data
public class DriverDTO { // Only fields needed for assignment
    private Long driverId;
    // Add location if needed for proximity logic
    // private Double currentLatitude;
    // private Double currentLongitude;
}