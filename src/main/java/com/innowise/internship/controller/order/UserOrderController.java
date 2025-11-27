package com.innowise.internship.controller.order;


import com.innowise.internship.dto.order.CreateOrderDto;
import com.innowise.internship.dto.order.OrderWithUserDto;
import com.innowise.internship.dto.order.UpdateOrderDto;
import com.innowise.internship.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('USER')")
public class UserOrderController {

    private final OrderService orderService;

    public UserOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(@Valid @RequestBody CreateOrderDto dto, Authentication authentication) {
        String email = authentication.getName();
        dto.setUserEmail(email);
        OrderWithUserDto created = orderService.createOrder(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderWithUserDto>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        List<OrderWithUserDto> orders = orderService.getOrdersByUserEmail(email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getMyOrderById(@PathVariable Long id, Authentication authentication) {
        OrderWithUserDto order = orderService.getOrderById(id);
        if (!order.getUser().getEmail().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateMyOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderDto dto, Authentication authentication) {
        OrderWithUserDto existing = orderService.getOrderById(id);
        if (!existing.getUser().getEmail().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        OrderWithUserDto updated = orderService.updateOrderById(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyOrder(@PathVariable Long id, Authentication authentication) {
        OrderWithUserDto existing = orderService.getOrderById(id);
        if (!existing.getUser().getEmail().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }
}