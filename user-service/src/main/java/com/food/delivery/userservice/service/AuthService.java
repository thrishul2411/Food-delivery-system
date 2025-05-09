package com.food.delivery.userservice.service;

import com.food.delivery.userservice.dto.AuthRequestDTO;
import com.food.delivery.userservice.dto.AuthResponseDTO;
import com.food.delivery.userservice.dto.RegisterRequestDTO;
import com.food.delivery.userservice.entity.User;
import com.food.delivery.userservice.repository.UserRepository;
import com.food.delivery.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailService userDetailService; // For loading UserDetails

    @Transactional
    public User registerUser(RegisterRequestDTO registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already in use!");

        }

        User user = new User(
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()), // Hash the password
                registerRequest.getFirstName(),
                registerRequest.getLastName()
        );
        return userRepository.save(user);
    }

    public AuthResponseDTO loginUser(AuthRequestDTO authRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = userDetailService.loadUserByUsername(authentication.getName());

        String jwt = jwtUtil.generateToken(userDetails.getUsername());

        return new AuthResponseDTO(jwt, userDetails.getUsername());
    }
}