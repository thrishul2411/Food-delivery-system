package com.food.delivery.restaurantservice.repository;

import com.food.delivery.restaurantservice.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    // Find all menu items for a specific restaurant
    List<MenuItem> findByRestaurantId(Long restaurantId);
}