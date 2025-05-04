package com.food.delivery.orderservice.repository;

import com.food.delivery.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Usually interact via Order entity's cascade operations
}