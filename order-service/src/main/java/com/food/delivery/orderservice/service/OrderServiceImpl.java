package com.food.delivery.orderservice.service;

import com.food.delivery.orderservice.client.RestaurantServiceClient;
import com.food.delivery.orderservice.dto.*;
import com.food.delivery.orderservice.entity.Order;
import com.food.delivery.orderservice.entity.OrderItem;
import com.food.delivery.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.food.delivery.orderservice.event.PaymentOutcomeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional; // Or jakarta.transactional
import java.util.function.Consumer;
import java.util.Optional; // Make sure Optional is imported
import com.food.delivery.orderservice.entity.Order;
import org.springframework.cloud.stream.function.StreamBridge;
import com.food.delivery.orderservice.event.publisher.OrderReadyForPickupEvent; // Correct import

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

    @Autowired
    private StreamBridge streamBridge; // Inject StreamBridge

    private static final String ORDER_READY_BINDING_NAME = "orderReadySupplier-out-0";

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

    // Spring Cloud Stream finds this @Bean and links it to the 'paymentOutcomeConsumer-in-0' binding
    // defined in application properties because the bean name matches the function definition.
    @Bean
    public Consumer<PaymentOutcomeEvent> paymentOutcomeConsumer() {
        return event -> {
            // Log that an event was received
            System.out.println("ORDER-SERVICE: Received PaymentOutcomeEvent: " + event);

            // Find the order mentioned in the event
            Optional<Order> optionalOrder = orderRepository.findById(event.getOrderId());

            // Process only if the order exists
            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();

                // Check current order status to prevent overwriting a later status
                // Example: Only process if PENDING_PAYMENT or RECEIVED (adjust as needed)
                // This avoids applying a FAILED status if the order was already DELIVERED, for example.
                List<String> processableStatus = Arrays.asList("PENDING_PAYMENT", "RECEIVED"); // Define states where payment outcome is relevant

                if (processableStatus.contains(order.getStatus())) {
                    // Use a separate transactional method for the database update
                    updateOrderStatusBasedOnPayment(order, event);
                } else {
                    System.out.println("ORDER-SERVICE: Order " + order.getId() +
                            " not in processable state for payment outcome (current: " +
                            order.getStatus() + "). Ignoring event.");
                }
            } else {
                // Log an error if the order ID from the event doesn't exist
                System.err.println("ORDER-SERVICE: Received payment event for unknown order ID: " + event.getOrderId());
                // Future: Consider adding to a dead-letter queue or alerting mechanism
            }
        };
    }

    // --- NEW: Transactional Method to Update Order Status ---
//    // Separating the DB logic makes transactions clearer
//    @Transactional
//    public void updateOrderStatusBasedOnPayment(Order order, PaymentOutcomeEvent event) {
//        System.out.println("ORDER-SERVICE: Updating status for order ID: " + order.getId() + " based on payment status: " + event.getStatus());
//        if ("SUCCESSFUL".equalsIgnoreCase(event.getStatus())) {
//            // Define what happens on successful payment
//            // Options: CONFIRMED, PREPARING, READY_FOR_PICKUP?
//            // Let's set it to PREPARING for now.
//            order.setStatus("PREPARING");
//            System.out.println("ORDER-SERVICE: Set status to PREPARING for order " + order.getId());
//            // TODO: In a later step, PUBLISH an OrderPaid or OrderReadyForPreparation event here.
//        } else { // FAILED or any other non-successful status
//            order.setStatus("PAYMENT_FAILED");
//            System.out.println("ORDER-SERVICE: Set status to PAYMENT_FAILED for order " + order.getId());
//            // TODO: Potentially publish an OrderCancelled event or trigger notification.
//        }
//        // Save the updated order status
//        orderRepository.save(order);
//        System.out.println("ORDER-SERVICE: Order " + order.getId() + " saved with status " + order.getStatus());
//    }

    @Transactional
    public void updateOrderStatusBasedOnPayment(Order order, PaymentOutcomeEvent event) {
        System.out.println("ORDER-SERVICE: Updating status for order ID: " + order.getId() + " based on payment status: " + event.getStatus());
        String previousStatus = order.getStatus(); // Store previous status if needed

        if ("SUCCESSFUL".equalsIgnoreCase(event.getStatus())) {
            order.setStatus("PREPARING"); // Set status to PREPARING
            System.out.println("ORDER-SERVICE: Set status to PREPARING for order " + order.getId());

            // *** PUBLISH OrderReadyForPickupEvent ***
            OrderReadyForPickupEvent readyEvent = OrderReadyForPickupEvent.builder()
                    .orderId(order.getId())
                    .restaurantId(order.getRestaurantId())
                    // Add restaurant location details if available/needed
                    .build();
            System.out.println("ORDER-SERVICE: Publishing event to binding [" + ORDER_READY_BINDING_NAME + "]: " + readyEvent);
            streamBridge.send(ORDER_READY_BINDING_NAME, readyEvent);
            // ****************************************

        } else {
            order.setStatus("PAYMENT_FAILED");
            System.out.println("ORDER-SERVICE: Set status to PAYMENT_FAILED for order " + order.getId());
        }
        orderRepository.save(order);
        System.out.println("ORDER-SERVICE: Order " + order.getId() + " saved with status " + order.getStatus());
    }
}