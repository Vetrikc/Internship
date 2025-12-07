package com.innowise.orderservice.integration;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.CreateOrderItemDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@Rollback
class OrderServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orderdb")
            .withUsername("test")
            .withPassword("test");


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.order.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.order.username", postgres::getUsername);
        registry.add("spring.datasource.order.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private OrderRepository orderRepository;

    private Item laptop;
    private Item mouse;

    @BeforeEach
    void setUp() {
        laptop = new Item();
        laptop.setName("Laptop Pro");
        laptop.setPrice(new BigDecimal("1499.99"));
        laptop = itemRepository.save(laptop);

        mouse = new Item();
        mouse.setName("Wireless Mouse");
        mouse.setPrice(new BigDecimal("59.99"));
        mouse = itemRepository.save(mouse);
    }

    @Test
    void createOrder_ShouldCreateWithItemsAndCalculateTotal() {
        CreateOrderItemDto item1 = new CreateOrderItemDto();
        item1.setItemId(laptop.getId());
        item1.setQuantity(1);

        CreateOrderItemDto item2 = new CreateOrderItemDto();
        item2.setItemId(mouse.getId());
        item2.setQuantity(2);

        CreateOrderDto dto = new CreateOrderDto();
        dto.setUserId(100L);
        dto.setOrderItems(List.of(item1, item2));
        dto.setStatus("status");

        OrderDto result = orderService.createOrder(dto);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1619.97")); // 1499.99 + 2*59.99

        Order fromDb = orderRepository.findById(result.getId()).get();
        assertThat(fromDb.getOrderItems()).hasSize(2);
        assertThat(fromDb.getDeleted()).isFalse();
    }

    @Test
    void createOrder_WhenItemNotFound_ShouldThrowException() {
        CreateOrderItemDto itemDto = new CreateOrderItemDto();
        itemDto.setItemId(999L);
        itemDto.setQuantity(1);

        CreateOrderDto dto = new CreateOrderDto();
        dto.setUserId(100L);
        dto.setOrderItems(List.of(itemDto));

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> orderService.createOrder(dto));
    }

    @Test
    void getOrderById_ShouldReturnOrderWithItems() {
        Order order = createSampleOrder();

        OrderDto result = orderService.getOrderById(order.getId());

        assertThat(result.getId()).isEqualTo(order.getId());
        assertThat(result.getOrderItems()).hasSize(2);
    }

    @Test
    void getOrderById_WhenNotFound_ShouldThrowException() {
        assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.getOrderById(999L));
    }

    @Test
    void getOrdersWithPaginationAndFilter_ShouldApplyFilters() {
        createSampleOrder(); // status = NEW
        createSampleOrder(); // ещё один

        Page<OrderDto> page = orderService.getOrdersWithPaginationAndFilter(
                PageRequest.of(0, 10),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                List.of("NEW")
        );

        assertThat(page.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void getOrdersByUserId_ShouldReturnOnlyUserOrders() {
        createSampleOrder(100L);
        createSampleOrder(200L);

        List<OrderDto> result = orderService.getOrdersByUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(100L);
    }

    @Test
    void updateOrderById_ShouldUpdateUserStatusAndReplaceItems() {
        Order order = createSampleOrder();

        UpdateOrderDto updateDto = new UpdateOrderDto();
        updateDto.setUserId(999L);
        updateDto.setStatus("SHIPPED");

        CreateOrderItemDto newItem = new CreateOrderItemDto();
        newItem.setItemId(mouse.getId());
        newItem.setQuantity(10);
        updateDto.setOrderItems(List.of(newItem));

        OrderDto updated = orderService.updateOrderById(order.getId(), updateDto);
        /*for (int i = 0; i < 10; i++) {
            System.out.println(updateDto.getOrderItems().get(i).getItemId());
        }*/

        assertThat(updated.getUserId()).isEqualTo(999L);
        assertThat(updated.getStatus()).isEqualTo("SHIPPED");
        assertThat(updated.getOrderItems()).hasSize(1);
        assertThat(updated.getOrderItems().getFirst().getQuantity()).isEqualTo(10);
        assertThat(updated.getTotalPrice()).isEqualByComparingTo(new BigDecimal("599.90")); // 10 * 59.99

        Order fromDb = orderRepository.findById(order.getId()).get();
        assertThat(fromDb.getOrderItems()).hasSize(1);
    }

    @Test
    void updateOrderById_WhenOnlyStatusChanged_ShouldNotTouchItems() {
        Order order = createSampleOrder();

        UpdateOrderDto updateDto = new UpdateOrderDto();
        updateDto.setStatus("CANCELLED");

        OrderDto result = orderService.updateOrderById(order.getId(), updateDto);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getOrderItems()).hasSize(2); // старые остались
    }

    @Test
    void updateOrderById_WhenItemNotFound_ShouldThrowException() {
        Order order = createSampleOrder();

        UpdateOrderDto updateDto = new UpdateOrderDto();
        CreateOrderItemDto badItem = new CreateOrderItemDto();
        badItem.setItemId(999L);
        updateDto.setOrderItems(List.of(badItem));

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> orderService.updateOrderById(order.getId(), updateDto));
    }

    @Test
    void deleteOrderById_ShouldSoftDelete() {
        Order order = createSampleOrder();

        orderService.deleteOrderById(order.getId());

        assertThat(orderRepository.findByIdAndDeletedFalse(order.getId())).isEmpty();
        assertThat(orderRepository.findById(order.getId())).isPresent();
        assertThat(orderRepository.findById(order.getId()).get().getDeleted()).isTrue();
    }

    @Test
    void deleteOrderById_WhenNotExists_ShouldNotThrow() {
        assertThatNoException().isThrownBy(() -> orderService.deleteOrderById(999L));
    }

    // Вспомогательный метод
    private Order createSampleOrder() {
        return createSampleOrder(100L);
    }

    private Order createSampleOrder(Long userId) {
        CreateOrderItemDto item1 = new CreateOrderItemDto();
        item1.setItemId(laptop.getId());
        item1.setQuantity(1);

        CreateOrderItemDto item2 = new CreateOrderItemDto();
        item2.setItemId(mouse.getId());
        item2.setQuantity(2);

        CreateOrderDto dto = new CreateOrderDto();
        dto.setUserId(userId);
        dto.setOrderItems(List.of(item1, item2));
        dto.setStatus("status");

        OrderDto result = orderService.createOrder(dto);
        return orderRepository.findById(result.getId()).get();
    }
}