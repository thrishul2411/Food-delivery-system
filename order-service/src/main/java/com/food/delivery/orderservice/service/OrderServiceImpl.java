package com.food.delivery.orderservice.service;

import com.food.delivery.orderservice.client.RestaurantServiceClient;
import com.food.delivery.orderservice.dto.*;
import com.food.delivery.orderservice.entity.Order;
import com.food.delivery.orderservice.entity.OrderItem;
import com.food.delivery.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional; // Use jakarta.transactional or Spring's
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl { // Renamed for convention

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantServiceClient restaurantServiceClient;

    // Simple DTO Mapper (Consider MapStruct later)
    private OrderResponseDTO mapOrderToResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setRestaurantId(order.getRestaurantId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setDeliveryAddressSnapshot(order.getDeliveryAddressSnapshot());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setItems(order.getItems().stream().map(this::mapOrderItemToResponseDTO).collect(Collectors.toList()));
        return dto;
    }

    private OrderItemResponseDTO mapOrderItemToResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setMenuItemId(item.getMenuItemId());
        dto.setItemName(item.getItemName());
        dto.setQuantity(item.getQuantity());
        dto.setPricePerItem(item.getPricePerItem());
        return dto;
    }


    @Transactional // Ensures operations are atomic
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Extract Item IDs for Validation
        Set<Long> menuItemIds = request.getItems().stream()
                .map(OrderItemRequestDTO::getMenuItemId)
                .collect(Collectors.toSet());

        // 2. Call Restaurant Service via Feign to get item details
        // TODO: Add error handling (e.g., Resilience4j circuit breaker) for this call
        List<MenuItemDTO> validatedMenuItemsList = restaurantServiceClient.getMenuItemsByIds(menuItemIds);

        // 3. Check if all requested items were returned/validated
        if (validatedMenuItemsList.size() != menuItemIds.size()) {
            // Find missing IDs (simple example)
            Set<Long> foundIds = validatedMenuItemsList.stream().map(MenuItemDTO::getId).collect(Collectors.toSet());
            menuItemIds.removeAll(foundIds); // Now contains only missing IDs
            throw new IllegalArgumentException("Invalid or unavailable menu items requested. Missing IDs: " + menuItemIds);
        }

        // Create a map for easy lookup by ID
        Map<Long, MenuItemDTO> validatedMenuItemsMap = validatedMenuItemsList.stream()
                .collect(Collectors.toMap(MenuItemDTO::getId, Function.identity()));


        // 4. Create Order Entity
        Order order = new Order();
        order.setUserId(1L); // Hardcoded for now - Get from SecurityContext later
        order.setRestaurantId(request.getRestaurantId());
        order.setDeliveryAddressSnapshot(request.getDeliveryAddress()); // Use requested address
        order.setStatus("RECEIVED"); // Initial status - adjust based on payment flow later
        // Total amount calculated below

        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // 5. Create OrderItem Entities and Calculate Total
        for (OrderItemRequestDTO itemRequest : request.getItems()) {
            MenuItemDTO validatedItem = validatedMenuItemsMap.get(itemRequest.getMenuItemId());
            if (validatedItem == null) { // Should not happen if previous check passed, but good practice
                throw new IllegalStateException("Validated item not found in map: " + itemRequest.getMenuItemId());
            }
            if(itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive for ID: " + itemRequest.getMenuItemId());
            }

            OrderItem orderItem = new OrderItem(
                    validatedItem.getId(),
                    validatedItem.getName(), // Get name from validated item
                    itemRequest.getQuantity(),
                    validatedItem.getPrice() // Get price from validated item
            );
            order.addItem(orderItem); // Adds item and sets bidirectional link

            // Calculate subtotal for this item and add to total
            totalOrderAmount = totalOrderAmount.add(
                    validatedItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }

        order.setTotalAmount(totalOrderAmount);

        // 6. Save Order (Cascade will save OrderItems)
        Order savedOrder = orderRepository.save(order);

        // 7. Map to Response DTO and return
        return mapOrderToResponseDTO(savedOrder);
    }

    // Add methods for getOrderById, getOrdersByUserId etc. later
    public Optional<OrderResponseDTO> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapOrderToResponseDTO);
    }

    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapOrderToResponseDTO)
                .collect(Collectors.toList());
    }
}