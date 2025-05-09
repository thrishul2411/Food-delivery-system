package com.food.delivery.deliveryservice.service;

import com.food.delivery.deliveryservice.client.DriverDTO;

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

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.HashOperations;

import java.util.Map;
import java.time.Duration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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
    private StringRedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String DELIVERY_OUTCOME_BINDING_NAME = "deliveryOutcomeSupplier-out-0";
    private static final String LOCATION_CACHE_PREFIX = "location:";
    private static final Duration LOCATION_CACHE_TTL = Duration.ofMinutes(5);

    @Bean
    public Consumer<OrderReadyForPickupEvent> orderReadyConsumer() {
        return event -> {
            System.out.println("DELIVERY-SERVICE: Received OrderReadyForPickupEvent: " + event);

            if (assignmentRepository.findByOrderId(event.getOrderId()).isPresent()) {
                System.out.println("DELIVERY-SERVICE: Order " + event.getOrderId() + " already has an assignment. Ignoring event.");
                return;
            }
            assignDriverToOrder(event.getOrderId());
        };
    }

    @Transactional
    public void assignDriverToOrder(Long orderId) {
        System.out.println("DELIVERY-SERVICE: Attempting to assign driver for order " + orderId);

        List<DriverDTO> availableDrivers;
        try {
            availableDrivers = driverServiceClient.getAvailableDrivers(true);
        } catch (Exception e) {

            System.err.println("DELIVERY-SERVICE: Error calling driver-service to find available drivers: " + e.getMessage());

            return;
        }

        if (availableDrivers.isEmpty()) {
            System.out.println("DELIVERY-SERVICE: No available drivers found for order " + orderId);

            return;
        }

        DriverDTO selectedDriver = availableDrivers.get(ThreadLocalRandom.current().nextInt(availableDrivers.size()));
        Long driverId = selectedDriver.getDriverId();
        System.out.println("DELIVERY-SERVICE: Selected driver " + driverId + " for order " + orderId);

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .driverId(driverId)
                .status("ASSIGNED")
                .build();
        assignmentRepository.save(assignment);
        System.out.println("DELIVERY-SERVICE: Saved assignment for order " + orderId + " with driver " + driverId);

        DriverAssignedEvent assignedEvent = DriverAssignedEvent.builder()
                .orderId(orderId)
                .driverId(driverId)
                .build();
        System.out.println("DELIVERY-SERVICE: Publishing event to binding [" + DELIVERY_OUTCOME_BINDING_NAME + "]: " + assignedEvent);
        streamBridge.send(DELIVERY_OUTCOME_BINDING_NAME, assignedEvent);
    }

    @Bean
    public Consumer<DriverLocationUpdatedEvent> driverLocationConsumer() {

        final HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();

        return event -> {

            System.out.println("DELIVERY-SERVICE: Received DriverLocationUpdatedEvent: " + event);

            Optional<DeliveryAssignment> activeAssignmentOpt = assignmentRepository
                    .findByDriverIdAndStatusIn(event.getDriverId(), Arrays.asList("ASSIGNED", "PICKED_UP"));

            if (activeAssignmentOpt.isPresent()) {
                Long orderId = activeAssignmentOpt.get().getOrderId();

                String cacheKey = LOCATION_CACHE_PREFIX + orderId;

                System.out.println("DELIVERY-SERVICE: Driver " + event.getDriverId() +
                        " has active assignment for order " + orderId +
                        ". Updating cache key: " + cacheKey);

                try {

                    hashOps.put(cacheKey, "latitude", String.valueOf(event.getLatitude()));
                    hashOps.put(cacheKey, "longitude", String.valueOf(event.getLongitude()));
                    hashOps.put(cacheKey, "timestamp", event.getTimestamp().toString()); // Store as ISO string

                    stringRedisTemplate.expire(cacheKey, LOCATION_CACHE_TTL);

                    System.out.println("DELIVERY-SERVICE: Updated Redis cache for key " + cacheKey +
                            " with TTL " + LOCATION_CACHE_TTL.toSeconds() + " seconds.");

                } catch (Exception e) {

                    System.err.println("DELIVERY-SERVICE: Failed to update Redis cache for key " + cacheKey + ": " + e.getMessage());

                }
                // TODO: Step 39 - Add logic here to push update via SSE to subscribed clients for this orderId
            } else {

                System.out.println("DELIVERY-SERVICE: Driver " + event.getDriverId() + " has no active assignment. Ignoring location update for caching.");

            }
        };
    }
    @Transactional
    public void updateDeliveryStatus(Long assignmentId, String newStatus) {

        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found: " + assignmentId));
        String currentStatus = assignment.getStatus();
        boolean statusChanged = false;
        LocalDateTime now = LocalDateTime.now();
        Object eventToPublish = null;
        boolean isFinalStatus = false;

        if ("PICKED_UP".equalsIgnoreCase(newStatus) && "ASSIGNED".equals(currentStatus)) {
            assignment.setStatus("PICKED_UP");
            assignment.setPickedUpAt(now);
            statusChanged = true;
            eventToPublish = OrderPickedUpEvent.builder()/*...*/.build();
            System.out.println("DELIVERY-SERVICE: Status updated to PICKED_UP for assignment " + assignmentId);
        }

        else if ("DELIVERED".equalsIgnoreCase(newStatus) && "PICKED_UP".equals(currentStatus)) {
            assignment.setStatus("DELIVERED");
            assignment.setDeliveredAt(now);
            statusChanged = true;
            eventToPublish = OrderDeliveredEvent.builder()/*...*/.build();
            System.out.println("DELIVERY-SERVICE: Status updated to DELIVERED for assignment " + assignmentId);
            isFinalStatus = true;
        }

        else if ("FAILED_DELIVERY".equalsIgnoreCase(newStatus)) {
            assignment.setStatus("FAILED_DELIVERY");

            statusChanged = true;
            isFinalStatus = true;

        }


        if (statusChanged) {
            assignmentRepository.save(assignment);
            if (eventToPublish != null) {
                System.out.println("DELIVERY-SERVICE: Publishing event to binding [" + DELIVERY_OUTCOME_BINDING_NAME + "]: " + eventToPublish);
                streamBridge.send(DELIVERY_OUTCOME_BINDING_NAME, eventToPublish);
            }

            if (isFinalStatus) {
                String cacheKey = LOCATION_CACHE_PREFIX + assignment.getOrderId();
                Boolean deleted = stringRedisTemplate.delete(cacheKey);
                System.out.println("DELIVERY-SERVICE: Delivery " + assignmentId + " reached final status. Cleared cache key " + cacheKey + "? " + deleted);
            }
        } else {
            System.out.println("DELIVERY-SERVICE: No status change needed for assignment " + assignmentId);
        }
    }

    public Optional<Map<Object, Object>> getCachedLocationForOrder(Long orderId) {
        String cacheKey = LOCATION_CACHE_PREFIX + orderId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey))) {
            Map<Object, Object> locationData = stringRedisTemplate.opsForHash().entries(cacheKey);
            return Optional.of(locationData);
        }
        return Optional.empty();
    }
}