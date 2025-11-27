package com.innowise.internship.controller.order;

import com.innowise.internship.dto.order.CreateOrderDto;
import com.innowise.internship.dto.order.OrderWithUserDto;
import com.innowise.internship.dto.order.UpdateOrderDto;
import com.innowise.internship.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(@Valid @RequestBody CreateOrderDto dto) {
        OrderWithUserDto created = orderService.createOrder(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserDto>> getOrdersWithPaginationAndFilter(
            Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> statuses) {
        Page<OrderWithUserDto> orders = orderService.getOrdersWithPaginationAndFilter(pageable, from, to, statuses);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getOrderById(@PathVariable Long id) {
        OrderWithUserDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderDto dto) {
        OrderWithUserDto updated = orderService.updateOrderById(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }
}