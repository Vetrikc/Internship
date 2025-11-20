package com.innowise.internship.dao;

import com.innowise.internship.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "paymentCards")
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    Optional<User> findByEmail(String email);
    Optional<User> findByName(String username);
}