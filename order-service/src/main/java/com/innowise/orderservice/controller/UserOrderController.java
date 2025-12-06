package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.*;
import com.innowise.orderservice.security.JwtPrincipal;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('USER')")
@EnableFeignClients
public class UserOrderController {

    private final OrderService orderService;
    private final UserServiceClient userServiceClient;

    public UserOrderController(OrderService orderService, UserServiceClient userServiceClient) {
        this.orderService = orderService;
        this.userServiceClient = userServiceClient;
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(@Valid @RequestBody CreateOrderDto dto, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        dto.setUserId(userId);

        OrderDto created = orderService.createOrder(dto);
        UserDto userDto = userServiceClient.getUserByEmail(authentication.getName());
        OrderWithUserDto orderWithUserDto = new OrderWithUserDto();
        orderWithUserDto.setOrder(created);
        orderWithUserDto.setUser(userDto);
        return new ResponseEntity<>(orderWithUserDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<OrdersWithUserDto> getMyOrders(Authentication authentication) {
        System.out.println(authentication.getName());

        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        UserDto userDto = userServiceClient.getUserByEmail(authentication.getName());
        OrdersWithUserDto ordersWithUserDto = new OrdersWithUserDto();
        ordersWithUserDto.setUser(userDto);
        ordersWithUserDto.setOrder(orders);

        return ResponseEntity.ok(ordersWithUserDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getMyOrderById(@PathVariable Long id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        OrderDto order = orderService.getOrderById(id);
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserDto userDto = userServiceClient.getUserByEmail(authentication.getName());
        OrderWithUserDto orderWithUserDto = new OrderWithUserDto();
        orderWithUserDto.setOrder(order);
        orderWithUserDto.setUser(userDto);


        return ResponseEntity.ok(orderWithUserDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateMyOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderDto dto, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        OrderDto existing = orderService.getOrderById(id);
        if (!existing.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        OrderDto updated = orderService.updateOrderById(id, dto);
        UserDto userDto = userServiceClient.getUserByEmail(authentication.getName());
        OrderWithUserDto orderWithUserDto = new OrderWithUserDto();
        orderWithUserDto.setOrder(updated);
        orderWithUserDto.setUser(userDto);


        return ResponseEntity.ok(orderWithUserDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyOrder(@PathVariable Long id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        OrderDto existing = orderService.getOrderById(id);
        if (!existing.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }
}