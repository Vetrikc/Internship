package com.innowise.orderservice.service;

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
import com.innowise.orderservice.specification.OrderSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderCreateMapper orderCreateMapper;
    final private OrderRepository orderRepository;

    private final OrderItemCreateMapper orderItemCreateMapper;

    public OrderService(OrderRepository orderRepository, ItemRepository itemRepository,
                        OrderMapper orderMapper, OrderCreateMapper orderCreateMapper, OrderItemCreateMapper orderItemCreateMapper) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.orderMapper = orderMapper;
        this.orderCreateMapper = orderCreateMapper;
        this.orderItemCreateMapper = orderItemCreateMapper;
    }

    @Transactional
    public OrderDto createOrder(CreateOrderDto dto) {
        Order order = orderCreateMapper.toEntity(dto);
        for (OrderItem item : order.getOrderItems()) {
            Item fullItem = itemRepository.findById(item.getItem().getId())
                    .orElseThrow(() -> new ItemNotFoundException(item.getItem().getId()));
            item.setItem(fullItem);
            item.setOrder(order);
        }
        order.setUserId(dto.getUserId());
        order.calculateTotalPrice();
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toDto(order);
    }


    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersWithPaginationAndFilter(Pageable pageable, LocalDateTime from, LocalDateTime to, List<String> statuses) {
        Specification<Order> spec = OrderSpecifications.withCreationDateRangeAndStatuses(from, to, statuses)
                .and(OrderSpecifications.fetchOrderItemsAndItem());

        Page<Order> orders = orderRepository.findAll(spec, pageable);

        return orders.map(orderMapper::toDto);
    }

    @Transactional
    public List<OrderDto> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(userId);
        return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());

    }

    @Transactional
    public OrderDto updateOrderById(Long id, UpdateOrderDto updatedDto) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // 1. Смена пользователя по id (если передан)
        if (updatedDto.getUserId() != null) {
            order.setUserId(updatedDto.getUserId());
        }

        // 2. Смена статуса
        if (updatedDto.getStatus() != null) {
            order.setStatus(updatedDto.getStatus());
        }

        // 3. Полная замена позиций заказа
        if (updatedDto.getOrderItems() != null && !updatedDto.getOrderItems().isEmpty()) {
            // Собираем все itemId одним списком
            Set<Long> itemIds = updatedDto.getOrderItems().stream()
                    .map(CreateOrderItemDto::getItemId)
                    .collect(Collectors.toSet());

            // Один запрос на все товары
            Map<Long, Item> itemsMap = itemRepository.findAllById(itemIds).stream()
                    .collect(Collectors.toMap(Item::getId, Function.identity()));

            // Проверка на отсутствие товаров
            Set<Long> missingIds = itemIds.stream()
                    .filter(misId -> !itemsMap.containsKey(misId))
                    .collect(Collectors.toSet());

            if (!missingIds.isEmpty()) {
                throw new ItemNotFoundException(missingIds.iterator().next());
            }

//             Удаляем старые позиции (orphanRemoval=true сделает DELETE в БД)
            order.getOrderItems().clear();

            // Добавляем новые
            for (CreateOrderItemDto itemDto : updatedDto.getOrderItems()) {
                Item fullItem = itemsMap.get(itemDto.getItemId());

                OrderItem orderItem = orderItemCreateMapper.toEntity(itemDto);
                // MapStruct уже установил orderItem.item.id = itemDto.itemId
                // Перезаписываем весь объект Item полной сущностью
                orderItem.setItem(fullItem);
                // ← САМОЕ ВАЖНОЕ: фиксируем цену на момент обновления заказа
                //orderItem.setPriceAtOrder(fullItem.getPrice());

                orderItem.setOrder(order);
                order.getOrderItems().add(orderItem);
            }
        }
        order.calculateTotalPrice();
        orderRepository.saveAndFlush(order);

        Order fullOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return orderMapper.toDto(fullOrder);
    }

    @Transactional
    public void deleteOrderById(Long id) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setDeleted(true);
            orderRepository.save(order);
        });
    }
}