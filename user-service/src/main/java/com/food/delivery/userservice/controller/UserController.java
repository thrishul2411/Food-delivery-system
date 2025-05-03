package com.food.delivery.userservice.controller;

import com.food.delivery.userservice.entity.User;
import com.food.delivery.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Marks this class as a REST controller (combines @Controller and @ResponseBody)
@RequestMapping("/api/users") // Base path for all endpoints in this controller
public class UserController {

    @Autowired // Spring automatically injects an instance of UserRepository
    private UserRepository userRepository;

    // Simple ping endpoint for testing
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("User Service is running!");
    }

    // Example: Get all users (for testing, remove/secure later)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Example: Create a dummy user (for testing, use proper DTOs/validation later)
    @PostMapping("/dummy")
    public User createDummyUser() {
        // WARNING: Storing plain text passwords! Add hashing ASAP.
        User user = new User("dummy." + System.currentTimeMillis() + "@example.com", "password123", "Dummy", "User");
        return userRepository.save(user);
    }
}