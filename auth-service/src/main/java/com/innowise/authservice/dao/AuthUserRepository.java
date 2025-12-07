package com.innowise.authservice.dao;

import com.innowise.authservice.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long>{
    Optional<AuthUser> findByEmail(String email);
    Boolean existsByEmail(String email);
}
