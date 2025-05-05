package com.food.delivery.deliveryservice.repository;

import com.food.delivery.deliveryservice.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    Optional<DeliveryAssignment> findByDriverIdAndStatusIn(Long driverId, List<String> statuses); // Find active assignment for driver
}