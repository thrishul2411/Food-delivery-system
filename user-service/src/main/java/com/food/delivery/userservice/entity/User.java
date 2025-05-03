package com.food.delivery.userservice.entity;

import jakarta.persistence.*;
import lombok.Data; // Lombok annotation for getters, setters, toString, etc.
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity // Marks this class as a JPA entity (corresponds to a database table)
@Table(name = "users") // Specifies the table name in the database
@Data // Lombok: Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Lombok: Generates a no-argument constructor (required by JPA)
public class User {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates the ID value (database handles incrementing)
    @Column(name = "user_id") // Specifies the column name
    private Long id;

    @Column(nullable = false, unique = true) // Database constraints
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password; // Store hashed passwords only! We'll add hashing later.

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "created_at", updatable = false) // Should not be updated after creation
    private LocalDateTime createdAt;

    @PrePersist // Method to run before the entity is first saved
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Add constructors, other fields (roles, addresses etc.) later as needed
    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password; // Remember to hash this!
        this.firstName = firstName;
        this.lastName = lastName;
    }
}