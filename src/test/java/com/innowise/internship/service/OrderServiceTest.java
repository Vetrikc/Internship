/*
package com.innowise.internship.service;

import com.innowise.internship.dao_order.OrderRepository;
import com.innowise.internship.entity.Item;
import com.innowise.internship.entity.Order;
import com.innowise.internship.entity.OrderItem;
import com.innowise.internship.exception.OrderNotFoundException;
import com.innowise.internship.service.order.OrderService;
import com.innowise.internship.specification.OrderSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setPrice(BigDecimal.valueOf(10.00));

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setItem(testItem);
        testOrderItem.setQuantity(2);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus("PENDING");
        testOrder.setOrderItems(new ArrayList<>(Collections.singletonList(testOrderItem)));
        testOrder.calculateTotalPrice();
    }

    @Test
    void createOrderShouldSetOrderItemsAndCalculateTotalPriceAndSave() {
        Order newOrder = new Order();
        newOrder.setUserId(2L);
        newOrder.setStatus("NEW");
        OrderItem newItem = new OrderItem();
        newItem.setItem(testItem);
        newItem.setQuantity(3);
        newOrder.setOrderItems(new ArrayList<>(Collections.singletonList(newItem)));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order savedOrder = orderService.createOrder(newOrder);

        assertNotNull(savedOrder);
        assertEquals(BigDecimal.valueOf(30.00), savedOrder.getTotalPrice());
        assertEquals(newOrder, savedOrder.getOrderItems().get(0).getOrder());
        verify(orderRepository, times(1)).save(newOrder);
    }

    @Test
    void getOrderByIdShouldReturnOrderWhenExistsAndNotDeleted() {
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testOrder));

        Optional<Order> result = Optional.ofNullable(orderService.getOrderById(1L));

        assertTrue(result.isPresent());
        assertEquals(testOrder, result.get());
        verify(orderRepository, times(1)).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getOrdersWithPaginationAndFilterShouldReturnPagedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        List<String> statuses = Collections.singletonList("PENDING");

        Page<Order> expectedPage = new PageImpl<>(Collections.singletonList(testOrder));
        Specification<Order> spec = OrderSpecifications.withCreationDateRangeAndStatuses(from, to, statuses);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);


        Page<Order> result = orderService.getOrdersWithPaginationAndFilter(pageable, from, to, statuses);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testOrder, result.getContent().get(0));
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOrdersByUserIdShouldReturnListOfOrders() {
        List<Order> expectedOrders = Collections.singletonList(testOrder);

        when(orderRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(expectedOrders);

        List<Order> result = orderService.getOrdersByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderRepository, times(1)).findByUserIdAndDeletedFalse(1L);
    }

    @Test
    void updateOrderByIdShouldUpdateAndReturnUpdatedOrderWhenExists() {
        Order updatedOrder = new Order();
        updatedOrder.setUserId(3L);
        updatedOrder.setStatus("UPDATED");
        OrderItem updatedItem = new OrderItem();
        updatedItem.setItem(testItem);
        updatedItem.setQuantity(4);
        updatedOrder.setOrderItems(new ArrayList<>(Collections.singletonList(updatedItem)));

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Order> result = orderService.updateOrderById(1L, updatedOrder);

        assertTrue(result.isPresent());
        Order savedOrder = result.get();
        assertEquals(3L, savedOrder.getUserId());
        assertEquals("UPDATED", savedOrder.getStatus());
        assertEquals(1, savedOrder.getOrderItems().size());
        assertEquals(4, savedOrder.getOrderItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(40.00), savedOrder.getTotalPrice());
        verify(orderRepository, times(1)).findByIdAndDeletedFalse(1L);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void deleteOrderByIdShouldSetDeletedTrueAndSaveWhenExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.deleteOrderById(1L);

        assertTrue(testOrder.getDeleted());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void deleteOrderByIdShouldDoNothingWhenNotExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        orderService.deleteOrderById(1L);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderByIdShouldThrowException_whenOrderNotFound() {
        Long id = 1L;
        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(id));
    }
    @Test
    void updateOrderByIdShouldThrowException_whenOrderNotFound() {
        Long id = 1L;
        Order updatedOrder = new Order();
        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.updateOrderById(id, updatedOrder));
    }
}*/
