package com.food.delivery.restaurantservice.repository;

import com.food.delivery.restaurantservice.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    // Example: Find by cuisine type (Spring Data generates query)
    List<Restaurant> findByCuisineTypeContainingIgnoreCase(String cuisine);
    // Add search methods later
}