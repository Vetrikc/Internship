package com.innowise.internship.dao_order;

import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{
}
