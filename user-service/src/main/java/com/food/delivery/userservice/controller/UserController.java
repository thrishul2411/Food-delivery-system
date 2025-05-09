package com.food.delivery.userservice.controller;

import com.food.delivery.userservice.entity.User;
import com.food.delivery.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("User Service is running!");
    }


    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/dummy")
    public User createDummyUser() {

        User user = new User("dummy." + System.currentTimeMillis() + "@example.com", "password123", "Dummy", "User");
        return userRepository.save(user);
    }
}