package com.food.delivery.deliveryservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "DRIVER-SERVICE", path = "/api/drivers")
public interface DriverServiceClient {

    @GetMapping
    List<DriverDTO> getAvailableDrivers(@RequestParam("available") boolean available);
}