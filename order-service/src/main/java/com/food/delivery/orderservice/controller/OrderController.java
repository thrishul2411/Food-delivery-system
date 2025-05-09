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
    private OrderServiceImpl orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequest) {
        try {
            OrderResponseDTO createdOrder = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {

            System.err.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getCurrentUserOrders() {
        // TODO: Get actual userId from security context/JWT
        Long currentUserId = 1L;
        List<OrderResponseDTO> orders = orderService.getOrdersByUserId(currentUserId);
        return ResponseEntity.ok(orders);
    }
}