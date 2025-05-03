package com.food.delivery.userservice.repository;

import com.food.delivery.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marks this as a Spring Data repository bean
public interface UserRepository extends JpaRepository<User, Long> { // <EntityType, PrimaryKeyType>

    // Spring Data JPA automatically creates method implementations based on the method name
    // Example: Find a user by their email address
    Optional<User> findByEmail(String email);

    // Add other custom query methods here if needed later
}