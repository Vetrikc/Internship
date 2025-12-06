package com.innowise.orderservice.dao;

import com.innowise.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>  {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<Order> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    List<Order> findByUserIdAndDeletedFalse(Long userId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Page<Order> findAllByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<Order> findById(Long id);
}
