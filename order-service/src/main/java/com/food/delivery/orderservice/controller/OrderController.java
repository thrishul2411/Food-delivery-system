package com.food.delivery.orderservice.controller;

import com.food.delivery.orderservice.dto.OrderRequestDTO;
import com.food.delivery.orderservice.dto.OrderResponseDTO;
import com.food.delivery.orderservice.service.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderServiceImpl orderService; // Use implementation

    // POST /api/orders - Create a new order
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequest) {
        try {
            OrderResponseDTO createdOrder = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., item not found, bad quantity)
            return ResponseEntity.badRequest().body(null); // Basic error handling
        } catch (Exception e) {
            // Handle other unexpected errors
            System.err.println("Error creating order: " + e.getMessage()); // Log error
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // GET /api/orders/{orderId} - Get order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/orders - Get orders for the current user (hardcoded user 1 for now)
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getCurrentUserOrders() {
        // TODO: Get actual userId from security context/JWT
        Long currentUserId = 1L;
        List<OrderResponseDTO> orders = orderService.getOrdersByUserId(currentUserId);
        return ResponseEntity.ok(orders);
    }
}