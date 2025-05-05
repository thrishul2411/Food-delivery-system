package com.food.delivery.driverservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationUpdatedEvent {
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}