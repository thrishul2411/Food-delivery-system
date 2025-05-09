package com.food.delivery.orderservice.service;

import com.food.delivery.orderservice.client.RestaurantServiceClient;
import com.food.delivery.orderservice.dto.*;
import com.food.delivery.orderservice.entity.Order;
import com.food.delivery.orderservice.entity.OrderItem;
import com.food.delivery.orderservice.event.consumer.DriverAssignedEvent;
import com.food.delivery.orderservice.event.consumer.OrderDeliveredEvent;
import com.food.delivery.orderservice.event.consumer.OrderPickedUpEvent;
import com.food.delivery.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.food.delivery.orderservice.event.PaymentOutcomeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import java.util.function.Consumer;
import java.util.Optional;
import org.springframework.cloud.stream.function.StreamBridge;
import com.food.delivery.orderservice.event.publisher.OrderReadyForPickupEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantServiceClient restaurantServiceClient;

    @Autowired
    private StreamBridge streamBridge;

    private static final String ORDER_READY_BINDING_NAME = "orderReadySupplier-out-0";

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

    @Bean
    public Consumer<DriverAssignedEvent> driverAssignedConsumer() {
        return event -> {
            System.out.println("ORDER-SERVICE: Received DriverAssignedEvent: " + event);
            updateOrderStatus(event.getOrderId(), "ASSIGNED", Arrays.asList("PREPARING"));

            System.out.println("ORDER-SERVICE: Driver " + event.getDriverId() + " assigned to order " + event.getOrderId());
        };
    }

    @Bean
    public Consumer<OrderPickedUpEvent> orderPickedUpConsumer() {
        return event -> {
            System.out.println("ORDER-SERVICE: Received OrderPickedUpEvent: " + event);
            updateOrderStatus(event.getOrderId(), "OUT_FOR_DELIVERY", Arrays.asList("ASSIGNED"));
        };
    }

    @Bean
    public Consumer<OrderDeliveredEvent> orderDeliveredConsumer() {
        return event -> {
            System.out.println("ORDER-SERVICE: Received OrderDeliveredEvent: " + event);
            updateOrderStatus(event.getOrderId(), "DELIVERED", Arrays.asList("OUT_FOR_DELIVERY"));
        };
    }


    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus, List<String> expectedCurrentStatuses) {
        System.out.println("ORDER-SERVICE: Attempting to update status for order ID: " + orderId + " to " + newStatus);
        Optional<Order> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if (expectedCurrentStatuses.contains(order.getStatus())) {
                order.setStatus(newStatus);
                orderRepository.save(order);
                System.out.println("ORDER-SERVICE: Order " + orderId + " status updated to " + newStatus);

            } else {
                System.out.println("ORDER-SERVICE: Order " + orderId +
                        " not in expected status(es) " + expectedCurrentStatuses +
                        " (current: " + order.getStatus() + "). Ignoring status update to " + newStatus + ".");
            }
        } else {
            System.err.println("ORDER-SERVICE: Received event for unknown order ID: " + orderId + " while trying to update status to " + newStatus);
        }
    }

    @Transactional
    public void updateOrderStatusBasedOnPayment(Order order, PaymentOutcomeEvent event) {

        List<String> processableStatus = Arrays.asList("PENDING_PAYMENT", "RECEIVED");
        if (!processableStatus.contains(order.getStatus())) {
            System.out.println("ORDER-SERVICE: Order " + order.getId() +
                    " not in processable state for payment outcome (current: " +
                    order.getStatus() + "). Ignoring event.");
            return;
        }

        System.out.println("ORDER-SERVICE: Updating status for order ID: " + order.getId() + " based on payment status: " + event.getStatus());
        String previousStatus = order.getStatus();

        if ("SUCCESSFUL".equalsIgnoreCase(event.getStatus())) {
            order.setStatus("PREPARING");
            System.out.println("ORDER-SERVICE: Set status to PREPARING for order " + order.getId());

            OrderReadyForPickupEvent readyEvent = OrderReadyForPickupEvent.builder()
                    .orderId(order.getId())
                    .restaurantId(order.getRestaurantId())
                    .build();
            System.out.println("ORDER-SERVICE: Publishing event to binding [" + ORDER_READY_BINDING_NAME + "]: " + readyEvent);
            streamBridge.send(ORDER_READY_BINDING_NAME, readyEvent);

        } else {
            order.setStatus("PAYMENT_FAILED");
            System.out.println("ORDER-SERVICE: Set status to PAYMENT_FAILED for order " + order.getId());
        }
        orderRepository.save(order);
        System.out.println("ORDER-SERVICE: Order " + order.getId() + " saved with status " + order.getStatus());
    }



    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Extract Item IDs for Validation
        Set<Long> menuItemIds = request.getItems().stream()
                .map(OrderItemRequestDTO::getMenuItemId)
                .collect(Collectors.toSet());

        List<MenuItemDTO> validatedMenuItemsList = restaurantServiceClient.getMenuItemsByIds(menuItemIds);

        if (validatedMenuItemsList.size() != menuItemIds.size()) {

            Set<Long> foundIds = validatedMenuItemsList.stream().map(MenuItemDTO::getId).collect(Collectors.toSet());
            menuItemIds.removeAll(foundIds);
            throw new IllegalArgumentException("Invalid or unavailable menu items requested. Missing IDs: " + menuItemIds);
        }

        Map<Long, MenuItemDTO> validatedMenuItemsMap = validatedMenuItemsList.stream()
                .collect(Collectors.toMap(MenuItemDTO::getId, Function.identity()));


        Order order = new Order();
        order.setUserId(1L);
        order.setRestaurantId(request.getRestaurantId());
        order.setDeliveryAddressSnapshot(request.getDeliveryAddress());
        order.setStatus("RECEIVED");


        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        for (OrderItemRequestDTO itemRequest : request.getItems()) {
            MenuItemDTO validatedItem = validatedMenuItemsMap.get(itemRequest.getMenuItemId());
            if (validatedItem == null) {
                throw new IllegalStateException("Validated item not found in map: " + itemRequest.getMenuItemId());
            }
            if(itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive for ID: " + itemRequest.getMenuItemId());
            }

            OrderItem orderItem = new OrderItem(
                    validatedItem.getId(),
                    validatedItem.getName(),
                    itemRequest.getQuantity(),
                    validatedItem.getPrice()
            );
            order.addItem(orderItem);

            totalOrderAmount = totalOrderAmount.add(
                    validatedItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }

        order.setTotalAmount(totalOrderAmount);

        Order savedOrder = orderRepository.save(order);

        return mapOrderToResponseDTO(savedOrder);
    }

    public Optional<OrderResponseDTO> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapOrderToResponseDTO);
    }

    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapOrderToResponseDTO)
                .collect(Collectors.toList());
    }

    @Bean
    public Consumer<PaymentOutcomeEvent> paymentOutcomeConsumer() {
        return event -> {

            System.out.println("ORDER-SERVICE: Received PaymentOutcomeEvent: " + event);

            Optional<Order> optionalOrder = orderRepository.findById(event.getOrderId());

            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();


                List<String> processableStatus = Arrays.asList("PENDING_PAYMENT", "RECEIVED"); // Define states where payment outcome is relevant

                if (processableStatus.contains(order.getStatus())) {

                    updateOrderStatusBasedOnPayment(order, event);
                } else {
                    System.out.println("ORDER-SERVICE: Order " + order.getId() +
                            " not in processable state for payment outcome (current: " +
                            order.getStatus() + "). Ignoring event.");
                }
            } else {

                System.err.println("ORDER-SERVICE: Received payment event for unknown order ID: " + event.getOrderId());

            }
        };
    }


}