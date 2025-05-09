package com.food.delivery.userservice.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}