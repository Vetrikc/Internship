package com.innowise.internship.dao_order;

import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.OrderItem;
import com.innowise.internship.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem> {

}
