package com.innowise.orderservice.unit;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.CreateOrderItemDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.mapper.OrderCreateMapper;
import com.innowise.orderservice.mapper.OrderItemCreateMapper;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.service.OrderService;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderCreateMapper orderCreateMapper;
    @Mock
    private OrderItemCreateMapper orderItemCreateMapper;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderDto orderDto;
    private Item item1, item2;
    private OrderItem orderItem1, orderItem2;

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long ITEM_ID_1 = 10L;
    private static final Long ITEM_ID_2 = 20L;

    @BeforeEach
    void setUp() {
        item1 = new Item();
        item1.setId(ITEM_ID_1);
        item1.setName("Laptop");
        item1.setPrice(new BigDecimal("999.99"));

        item2 = new Item();
        item2.setId(ITEM_ID_2);
        item2.setName("Mouse");
        item2.setPrice(new BigDecimal("29.99"));

        orderItem1 = new OrderItem();
        orderItem1.setItem(item1);
        orderItem1.setQuantity(1);

        orderItem2 = new OrderItem();
        orderItem2.setItem(item2);
        orderItem2.setQuantity(2);

        order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus("NEW");
        order.setDeleted(false);
        order.setOrderItems(new ArrayList<>(List.of(orderItem1, orderItem2)));

        orderDto = new OrderDto();
        orderDto.setId(ORDER_ID);
        orderDto.setUserId(USER_ID);
        orderDto.setStatus("NEW");
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        CreateOrderItemDto itemDto1 = new CreateOrderItemDto();
        itemDto1.setItemId(ITEM_ID_1);
        itemDto1.setQuantity(1);

        CreateOrderItemDto itemDto2 = new CreateOrderItemDto();
        itemDto2.setItemId(ITEM_ID_2);
        itemDto2.setQuantity(2);

        CreateOrderDto createDto = new CreateOrderDto();
        createDto.setUserId(USER_ID);
        createDto.setOrderItems(List.of(itemDto1, itemDto2));

        Order emptyOrder = new Order();
        emptyOrder.setOrderItems(new ArrayList<>(List.of(orderItem1, orderItem2)));

        when(orderCreateMapper.toEntity(createDto)).thenReturn(emptyOrder);
        when(itemRepository.findById(ITEM_ID_1)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(ITEM_ID_2)).thenReturn(Optional.of(item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            saved.setId(ORDER_ID);
            return saved;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.createOrder(createDto);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());

        verify(orderCreateMapper).toEntity(createDto);
        verify(itemRepository, times(2)).findById(anyLong());
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(any(Order.class));
    }

    @Test
    void createOrder_WhenItemNotFound_ShouldThrowItemNotFoundException() {
        CreateOrderItemDto itemDto = new CreateOrderItemDto();
        itemDto.setItemId(999L);
        itemDto.setQuantity(1);

        CreateOrderDto createDto = new CreateOrderDto();
        createDto.setUserId(USER_ID);
        createDto.setOrderItems(List.of(itemDto));

        Order emptyOrder = new Order();
        OrderItem oi = new OrderItem();
        Item stubItem = new Item();
        stubItem.setId(999L);
        oi.setItem(stubItem);
        emptyOrder.setOrderItems(List.of(oi));

        when(orderCreateMapper.toEntity(createDto)).thenReturn(emptyOrder);
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.createOrder(createDto));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrderDto() {
        when(orderRepository.findByIdAndDeletedFalse(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        OrderDto result = orderService.getOrderById(ORDER_ID);

        assertEquals(ORDER_ID, result.getId());
        verify(orderRepository).findByIdAndDeletedFalse(ORDER_ID);
    }

    @Test
    void getOrderById_WhenNotFound_ShouldThrowOrderNotFoundException() {
        when(orderRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void getOrdersWithPaginationAndFilter_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        Page<OrderDto> result = orderService.getOrdersWithPaginationAndFilter(
                pageable, LocalDateTime.now().minusDays(7), LocalDateTime.now(), List.of("NEW"));

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOrdersByUserId_ShouldReturnList() {
        when(orderRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        List<OrderDto> result = orderService.getOrdersByUserId(USER_ID);

        assertEquals(1, result.size());
        verify(orderRepository).findByUserIdAndDeletedFalse(USER_ID);
    }

    @Test
    void updateOrderById_ShouldUpdateFieldsAndItems() {
        UpdateOrderDto updateDto = new UpdateOrderDto();
        updateDto.setUserId(200L);
        updateDto.setStatus("SHIPPED");

        CreateOrderItemDto newItemDto = new CreateOrderItemDto();
        newItemDto.setItemId(ITEM_ID_1);
        newItemDto.setQuantity(5);
        updateDto.setOrderItems(List.of(newItemDto));

        when(orderRepository.findByIdAndDeletedFalse(ORDER_ID)).thenReturn(Optional.of(order));
        when(itemRepository.findAllById(Set.of(ITEM_ID_1))).thenReturn(List.of(item1));
        when(orderItemCreateMapper.toEntity(newItemDto)).thenAnswer(inv -> {
            OrderItem oi = new OrderItem();
            oi.setQuantity(5);
            oi.setItem(item1);
            return oi;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        OrderDto result = orderService.updateOrderById(ORDER_ID, updateDto);

        assertEquals(200L, order.getUserId());
        assertEquals("SHIPPED", order.getStatus());
        assertEquals(1, order.getOrderItems().size());
        assertEquals(5, order.getOrderItems().get(0).getQuantity());

        verify(orderRepository).saveAndFlush(order);
        verify(orderRepository).findById(ORDER_ID);
    }

    @Test
    void updateOrderById_WhenItemNotFound_ShouldThrowException() {
        UpdateOrderDto updateDto = new UpdateOrderDto();
        CreateOrderItemDto itemDto = new CreateOrderItemDto();
        itemDto.setItemId(999L);
        updateDto.setOrderItems(List.of(itemDto));

        when(orderRepository.findByIdAndDeletedFalse(ORDER_ID)).thenReturn(Optional.of(order));
        when(itemRepository.findAllById(Set.of(999L))).thenReturn(List.of());

        assertThrows(ItemNotFoundException.class, () -> orderService.updateOrderById(ORDER_ID, updateDto));

        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteOrderById_WhenExists_ShouldMarkAsDeleted() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.deleteOrderById(ORDER_ID);

        assertTrue(order.getDeleted());
        verify(orderRepository).save(order);
    }

    @Test
    void deleteOrderById_WhenNotExists_ShouldDoNothing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        orderService.deleteOrderById(999L);

        verify(orderRepository, never()).save(any());
    }
}