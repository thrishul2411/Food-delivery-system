package com.food.delivery.userservice.repository;

import com.food.delivery.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { // <EntityType, PrimaryKeyType>

    Optional<User> findByEmail(String email);

}