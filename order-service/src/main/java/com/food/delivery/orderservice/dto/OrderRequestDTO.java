package com.food.delivery.orderservice.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OrderRequestDTO {
    // Assuming user ID comes from JWT/Security Context later
    // private Long userId;
    private Long restaurantId;
    private List<OrderItemRequestDTO> items;
    private String deliveryAddress; // Simple string for now

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public List<OrderItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDTO> items) {
        this.items = items;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}