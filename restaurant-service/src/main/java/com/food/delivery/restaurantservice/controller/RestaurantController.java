package com.food.delivery.restaurantservice.controller;

import com.food.delivery.restaurantservice.entity.MenuItem;
import com.food.delivery.restaurantservice.entity.Restaurant;
import com.food.delivery.restaurantservice.repository.MenuItemRepository;
import com.food.delivery.restaurantservice.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/restaurants") // Base path matching design doc
public class RestaurantController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    // GET /api/restaurants - List all restaurants (add search/filter later)
    @GetMapping
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    // GET /api/restaurants/{restaurantId} - Get specific restaurant details
    @GetMapping("/{restaurantId}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        return restaurant.map(ResponseEntity::ok) // If found, return 200 OK with body
                .orElseGet(() -> ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }

    // GET /api/restaurants/{restaurantId}/menu - Get menu for a specific restaurant
    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenuByRestaurantId(@PathVariable Long restaurantId) {
        // Check if restaurant exists first
        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build(); // Restaurant not found
        }
        List<MenuItem> menuItems = menuItemRepository.findByRestaurantId(restaurantId);
        return ResponseEntity.ok(menuItems);
    }

    // POST /api/restaurants/dummy - Add dummy data (for testing)
    @PostMapping("/dummy")
    @ResponseStatus(HttpStatus.CREATED) // Return 201 Created
    public void addDummyData() {
        if (restaurantRepository.count() == 0) { // Add only if empty
            Restaurant r1 = restaurantRepository.save(new Restaurant("Pizza Palace", "Classic pizzas", "123 Main St", "Italian"));
            Restaurant r2 = restaurantRepository.save(new Restaurant("Curry Corner", "Authentic Indian", "456 Side Ave", "Indian"));

            menuItemRepository.save(new MenuItem(r1.getId(), "Margherita Pizza", "Cheese and Tomato", new BigDecimal("12.99")));
            menuItemRepository.save(new MenuItem(r1.getId(), "Pepperoni Pizza", "Classic Pepperoni", new BigDecimal("14.50")));
            menuItemRepository.save(new MenuItem(r2.getId(), "Chicken Tikka Masala", "Creamy curry", new BigDecimal("15.95")));
            menuItemRepository.save(new MenuItem(r2.getId(), "Vegetable Korma", "Mild veggie curry", new BigDecimal("13.50")));
        }
    }

    @GetMapping("/internal/menu-items")
    public ResponseEntity<List<com.food.delivery.restaurantservice.entity.MenuItem>> getMenuItemsForOrder(
            @RequestParam("ids") Set<Long> ids) {
        // Important: Use the MenuItem entity from THIS service
        List<com.food.delivery.restaurantservice.entity.MenuItem> items = menuItemRepository.findAllById(ids);
        // Basic validation: Check if all requested IDs were found
        if (items.size() != ids.size()) {
            // Optionally log which items were not found
            System.err.println("Warning: Some menu item IDs not found for order validation.");
            // Decide if this should be an error or just return what was found.
            // For now, return what was found, OrderService can handle discrepancies.
        }
        // We return the full entity, Feign/Jackson will map it to MenuItemDTO in OrderService
        return ResponseEntity.ok(items);
    }

}