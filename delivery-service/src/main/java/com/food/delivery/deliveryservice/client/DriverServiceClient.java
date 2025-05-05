package com.food.delivery.deliveryservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

// Name must match Eureka registration (adjust case!)
@FeignClient(name = "DRIVER-SERVICE", path = "/api/drivers")
public interface DriverServiceClient {

    @GetMapping // Maps to GET /api/drivers
    List<DriverDTO> getAvailableDrivers(@RequestParam("available") boolean available);
}