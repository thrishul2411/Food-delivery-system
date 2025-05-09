package com.food.delivery.orderservice.client;

import com.food.delivery.orderservice.dto.MenuItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;


@FeignClient(name = "RESTAURANT-SERVICE", path = "/api/restaurants")
public interface RestaurantServiceClient {

    @GetMapping("/internal/menu-items")
    List<MenuItemDTO> getMenuItemsByIds(@RequestParam("ids") Set<Long> itemIds);


}