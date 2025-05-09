package com.food.delivery.restaurantservice.repository;

import com.food.delivery.restaurantservice.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByCuisineTypeContainingIgnoreCase(String cuisine);

}