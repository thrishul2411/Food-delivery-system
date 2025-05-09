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
@RequestMapping("/api/restaurants")
public class RestaurantController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @GetMapping
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        return restaurant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenuByRestaurantId(@PathVariable Long restaurantId) {

        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build();
        }
        List<MenuItem> menuItems = menuItemRepository.findByRestaurantId(restaurantId);
        return ResponseEntity.ok(menuItems);
    }

    @PostMapping("/dummy")
    @ResponseStatus(HttpStatus.CREATED)
    public void addDummyData() {
        if (restaurantRepository.count() == 0) {
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
        List<com.food.delivery.restaurantservice.entity.MenuItem> items = menuItemRepository.findAllById(ids);

        if (items.size() != ids.size()) {

            System.err.println("Warning: Some menu item IDs not found for order validation.");

        }

        return ResponseEntity.ok(items);
    }

}