package com.food.delivery.deliveryservice.controller;
import lombok.Data;
@Data
public class DeliveryStatusUpdateRequest {
    private String status; // e.g., "PICKED_UP", "DELIVERED"
}