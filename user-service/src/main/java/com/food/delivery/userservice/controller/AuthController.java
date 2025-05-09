package com.food.delivery.userservice.controller;

import com.food.delivery.userservice.dto.AuthRequestDTO;
import com.food.delivery.userservice.dto.AuthResponseDTO;
import com.food.delivery.userservice.dto.RegisterRequestDTO;
import com.food.delivery.userservice.entity.User;
import com.food.delivery.userservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            User registeredUser = authService.registerUser(registerRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully! Email: " + registeredUser.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody AuthRequestDTO authRequest) {
        try {
            AuthResponseDTO authResponse = authService.loginUser(authRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {

            System.err.println("Login error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed: Invalid credentials");
        }
    }
}