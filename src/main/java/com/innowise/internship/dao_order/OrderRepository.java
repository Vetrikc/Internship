package com.innowise.internship.dao_order;

import com.innowise.internship.entity.Order;
import com.innowise.internship.entity.User;
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

    Optional<Order> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByUserIdAndDeletedFalse(Long userId);

    @Query("SELECT o FROM Order o WHERE o.deleted = false")
    Page<Order> findAllByDeletedFalse(Pageable pageable);
}
