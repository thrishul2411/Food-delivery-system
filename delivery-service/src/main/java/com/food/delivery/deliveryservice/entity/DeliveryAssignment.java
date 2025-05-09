package com.food.delivery.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true) // One delivery per order
    private Long orderId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onAssign() {
        assignedAt = LocalDateTime.now();
        if (status == null) {
            status = "ASSIGNED"; // Default on creation
        }
    }
}