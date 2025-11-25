package com.innowise.internship.service.order;

import com.innowise.internship.dao_order.OrderRepository;
import com.innowise.internship.entity.Order;
import com.innowise.internship.entity.OrderItem;
import com.innowise.internship.exception.OrderNotFoundException;
import com.innowise.internship.specification.OrderSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    final private OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(order);
        }
        order.calculateTotalPrice();
        return orderRepository.save(order);
    }

    @Transactional
    public Order getOrderById(Long id) {
        return orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }


    @Transactional
    public Page<Order> getOrdersWithPaginationAndFilter(Pageable pageable, LocalDateTime from, LocalDateTime to, List<String> statuses) {
        return orderRepository.findAll(OrderSpecifications.withCreationDateRangeAndStatuses(from, to, statuses), pageable);
    }

    @Transactional
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdAndDeletedFalse(userId);
    }

    @Transactional
    public Optional<Order> updateOrderById(Long id, Order updatedOrder) {
        return orderRepository.findByIdAndDeletedFalse(id)
                .map(order -> {
                    order.setUserId(updatedOrder.getUserId());
                    order.setStatus(updatedOrder.getStatus());
                    order.getOrderItems().clear();
                    for (OrderItem item : updatedOrder.getOrderItems()) {
                        item.setOrder(order);
                        order.getOrderItems().add(item);
                    }
                    order.calculateTotalPrice();
                    return orderRepository.save(order);
                })
                .or(() -> {
                    throw new OrderNotFoundException(id);
                });
    }

    @Transactional
    public void deleteOrderById(Long id) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setDeleted(true);
            orderRepository.save(order);
        });
    }
}