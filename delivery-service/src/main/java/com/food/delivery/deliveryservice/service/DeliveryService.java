package com.food.delivery.deliveryservice.service;

import com.food.delivery.deliveryservice.client.DriverServiceClient;
import com.food.delivery.deliveryservice.entity.DeliveryAssignment;
import com.food.delivery.deliveryservice.event.consumer.DriverLocationUpdatedEvent;
import com.food.delivery.deliveryservice.event.consumer.OrderReadyForPickupEvent;
import com.food.delivery.deliveryservice.event.publisher.DriverAssignedEvent;
import com.food.delivery.deliveryservice.event.publisher.OrderDeliveredEvent;
import com.food.delivery.deliveryservice.event.publisher.OrderPickedUpEvent;
import com.food.delivery.deliveryservice.repository.DeliveryAssignmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.stream.function.StreamBridge;

import org.springframework.data.redis.core.StringRedisTemplate; // For Redis cache
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom; // For random driver selection
import java.util.function.Consumer;

@Service
public class DeliveryService {

    @Autowired
    private DeliveryAssignmentRepository assignmentRepository;
    @Autowired
    private DriverServiceClient driverServiceClient;
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private StringRedisTemplate redisTemplate; // Inject Redis Template

    private static final String DELIVERY_OUTCOME_BINDING_NAME = "deliveryOutcomeSupplier-out-0";
    private static final String LOCATION_CACHE_PREFIX = "location:";
    private static final Duration LOCATION_CACHE_TTL = Duration.ofMinutes(5); // Cache TTL

    // --- Consume Order Ready Event ---
    @Bean
    public Consumer<OrderReadyForPickupEvent> orderReadyConsumer() {
        return event -> {
            System.out.println("DELIVERY-SERVICE: Received OrderReadyForPickupEvent: " + event);
            // Prevent assigning if already assigned
            if (assignmentRepository.findByOrderId(event.getOrderId()).isPresent()) {
                System.out.println("DELIVERY-SERVICE: Order " + event.getOrderId() + " already has an assignment. Ignoring event.");
                return;
            }
            assignDriverToOrder(event.getOrderId());
        };
    }

    @Transactional // Make assignment transactional
    public void assignDriverToOrder(Long orderId) {
        System.out.println("DELIVERY-SERVICE: Attempting to assign driver for order " + orderId);
        // 1. Find available drivers via Feign client
        List<DriverDTO> availableDrivers;
        try {
            availableDrivers = driverServiceClient.getAvailableDrivers(true);
        } catch (Exception e) {
            // Handle Feign errors (e.g., driver-service down)
            System.err.println("DELIVERY-SERVICE: Error calling driver-service to find available drivers: " + e.getMessage());
            // TODO: Implement retry logic or alternative flow (e.g., queue for later assignment)
            return;
        }

        // 2. Simple Assignment Logic: Pick a random available driver
        // TODO: Replace with proximity logic using restaurant/driver locations
        if (availableDrivers.isEmpty()) {
            System.out.println("DELIVERY-SERVICE: No available drivers found for order " + orderId);
            // TODO: Handle scenario - requeue, notify admin, etc.
            return;
        }

        DriverDTO selectedDriver = availableDrivers.get(ThreadLocalRandom.current().nextInt(availableDrivers.size()));
        Long driverId = selectedDriver.getDriverId();
        System.out.println("DELIVERY-SERVICE: Selected driver " + driverId + " for order " + orderId);

        // 3. Create and save assignment
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .driverId(driverId)
                .status("ASSIGNED") // Initial status
                .build();
        assignmentRepository.save(assignment);
        System.out.println("DELIVERY-SERVICE: Saved assignment for order " + orderId + " with driver " + driverId);

        // 4. Publish DriverAssignedEvent
        DriverAssignedEvent assignedEvent = DriverAssignedEvent.builder()
                .orderId(orderId)
                .driverId(driverId)
                .build();
        System.out.println("DELIVERY-SERVICE: Publishing event to binding [" + DELIVERY_OUTCOME_BINDING_NAME + "]: " + assignedEvent);
        streamBridge.send(DELIVERY_OUTCOME_BINDING_NAME, assignedEvent);
    }

    // --- Consume Driver Location Event ---
    @Bean
    public Consumer<DriverLocationUpdatedEvent> driverLocationConsumer() {
        return event -> {
            System.out.println("DELIVERY-SERVICE: Received DriverLocationUpdatedEvent: " + event);
            // Check if this driver has an active assignment (ASSIGNED or PICKED_UP)
            Optional<DeliveryAssignment> activeAssignment = assignmentRepository
                    .findByDriverIdAndStatusIn(event.getDriverId(), Arrays.asList("ASSIGNED", "PICKED_UP"));

            if (activeAssignment.isPresent()) {
                Long orderId = activeAssignment.get().getOrderId();
                String cacheKey = LOCATION_CACHE_PREFIX + orderId; // Use order ID or assignment ID for cache key
                System.out.println("DELIVERY-SERVICE: Driver " + event.getDriverId() + " has active assignment for order " + orderId + ". Updating cache key: " + cacheKey);

                // Update Redis Cache (Hash)
                try {
                    redisTemplate.opsForHash().put(cacheKey, "latitude", String.valueOf(event.getLatitude()));
                    redisTemplate.opsForHash().put(cacheKey, "longitude", String.valueOf(event.getLongitude()));
                    redisTemplate.opsForHash().put(cacheKey, "timestamp", event.getTimestamp().toString());
                    // Set TTL (Time To Live) for the cache entry
                    redisTemplate.expire(cacheKey, LOCATION_CACHE_TTL);
                    System.out.println("DELIVERY-SERVICE: Updated Redis cache for key " + cacheKey);
                } catch (Exception e) {
                    System.err.println("DELIVERY-SERVICE: Failed to update Redis cache for key " + cacheKey + ": " + e.getMessage());
                    // Handle Redis connection errors
                }
                // TODO: In Step 39, add logic here to push update via SSE
            } else {
                System.out.println("DELIVERY-SERVICE: Driver " + event.getDriverId() + " has no active assignment. Ignoring location update.");
            }
        };
    }

    // --- Service method called by Controller to update status ---
    @Transactional
    public void updateDeliveryStatus(Long assignmentId, String newStatus) {
        System.out.println("DELIVERY-SERVICE: Attempting to update status for assignment " + assignmentId + " to " + newStatus);
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found: " + assignmentId));

        // Basic state validation
        String currentStatus = assignment.getStatus();
        boolean statusChanged = false;
        LocalDateTime now = LocalDateTime.now();
        Object eventToPublish = null;

        if ("PICKED_UP".equalsIgnoreCase(newStatus) && "ASSIGNED".equals(currentStatus)) {
            assignment.setStatus("PICKED_UP");
            assignment.setPickedUpAt(now);
            statusChanged = true;
            eventToPublish = OrderPickedUpEvent.builder()
                    .orderId(assignment.getOrderId())
                    .driverId(assignment.getDriverId())
                    .timestamp(now)
                    .build();
            System.out.println("DELIVERY-SERVICE: Status updated to PICKED_UP for assignment " + assignmentId);
        } else if ("DELIVERED".equalsIgnoreCase(newStatus) && "PICKED_UP".equals(currentStatus)) {
            assignment.setStatus("DELIVERED");
            assignment.setDeliveredAt(now);
            statusChanged = true;
            eventToPublish = OrderDeliveredEvent.builder()
                    .orderId(assignment.getOrderId())
                    .driverId(assignment.getDriverId())
                    .timestamp(now)
                    .build();
            System.out.println("DELIVERY-SERVICE: Status updated to DELIVERED for assignment " + assignmentId);
            // TODO: Clean up location cache? Maybe not immediately.
        } else if (!newStatus.equalsIgnoreCase(currentStatus)) {
            // Allow other status updates potentially? Or throw error?
            System.out.println("DELIVERY-SERVICE: Status update from " + currentStatus + " to " + newStatus + " for assignment " + assignmentId);
            // For now, just update status field if it's different
            assignment.setStatus(newStatus);
            statusChanged = true;
            // Decide if other status changes should publish events
        }

        if (statusChanged) {
            assignmentRepository.save(assignment);
            if (eventToPublish != null) {
                System.out.println("DELIVERY-SERVICE: Publishing event to binding [" + DELIVERY_OUTCOME_BINDING_NAME + "]: " + eventToPublish);
                streamBridge.send(DELIVERY_OUTCOME_BINDING_NAME, eventToPublish);
            }
        } else {
            System.out.println("DELIVERY-SERVICE: No status change needed for assignment " + assignmentId);
        }
    }
}