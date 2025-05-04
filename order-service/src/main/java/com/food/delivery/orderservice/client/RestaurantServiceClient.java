package com.food.delivery.orderservice.client;

import com.food.delivery.orderservice.dto.MenuItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam; // If using request params

import java.util.List;
import java.util.Set;


// name = Service ID registered in Eureka (case-sensitive if not using lowercase locator)
// path = Optional base path for ALL requests in this client (matches controller @RequestMapping)
@FeignClient(name = "RESTAURANT-SERVICE", path = "/api/restaurants") // Adjust name case if needed!
public interface RestaurantServiceClient {

    // Option 1: Endpoint to get multiple items by ID (preferred)
    // This requires a corresponding endpoint on RestaurantController
    // Example: GET /api/restaurants/internal/menu-items?ids=1,2,3
    @GetMapping("/internal/menu-items")
    List<MenuItemDTO> getMenuItemsByIds(@RequestParam("ids") Set<Long> itemIds);

    // Option 2: Endpoint to get a single item by ID (less efficient for multiple items)
    // This requires a corresponding endpoint on RestaurantController
    // Example: GET /api/restaurants/internal/menu-items/{itemId}
    // @GetMapping("/internal/menu-items/{itemId}")
    // MenuItemDTO getMenuItemById(@PathVariable("itemId") Long itemId);
}