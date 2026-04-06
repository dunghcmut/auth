package com.example.auth.repository;

import com.example.auth.entity.User;
import com.example.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);
}
