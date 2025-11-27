package com.innowise.internship.service.order;

import com.innowise.internship.dao_order.ItemRepository;
import com.innowise.internship.dao_order.OrderRepository;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.dto.order.CreateOrderDto;
import com.innowise.internship.dto.order.CreateOrderItemDto;
import com.innowise.internship.dto.order.OrderWithUserDto;
import com.innowise.internship.dto.order.UpdateOrderDto;
import com.innowise.internship.entity.Item;
import com.innowise.internship.entity.Order;
import com.innowise.internship.entity.OrderItem;
import com.innowise.internship.exception.ItemNotFoundException;
import com.innowise.internship.exception.OrderNotFoundException;
import com.innowise.internship.mapper.order.OrderCreateMapper;
import com.innowise.internship.mapper.order.OrderItemCreateMapper;
import com.innowise.internship.mapper.order.OrderMapper;
import com.innowise.internship.service.UserService;
import com.innowise.internship.specification.OrderSpecifications;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final OrderCreateMapper orderCreateMapper;
    final private OrderRepository orderRepository;

    private final OrderItemCreateMapper orderItemCreateMapper;

    public OrderService(OrderRepository orderRepository, ItemRepository itemRepository, UserService userService,
                        OrderMapper orderMapper, OrderCreateMapper orderCreateMapper, OrderItemCreateMapper orderItemCreateMapper) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.userService = userService;
        this.orderMapper = orderMapper;
        this.orderCreateMapper = orderCreateMapper;
        this.orderItemCreateMapper = orderItemCreateMapper;
    }

    @Transactional
    public OrderWithUserDto createOrder(CreateOrderDto dto) {
        UserDto userDto = userService.getUserByEmail(dto.getUserEmail()).get();

        Order order = orderCreateMapper.toEntity(dto);
        for (OrderItem item : order.getOrderItems()) {
            Item fullItem = itemRepository.findById(item.getItem().getId())
                    .orElseThrow(() -> new ItemNotFoundException(item.getItem().getId()));
            item.setItem(fullItem);
            item.setOrder(order);
        }
        order.setUserId(userDto.getId());
        order.calculateTotalPrice();
        Order saved = orderRepository.save(order);
        return new OrderWithUserDto(orderMapper.toDto(saved), userDto);
    }

    @Transactional
    public OrderWithUserDto getOrderById(Long id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        UserDto user = getUserById(order.getUserId());
        return new OrderWithUserDto(orderMapper.toDto(order), user);
    }


    @Transactional
    public Page<OrderWithUserDto> getOrdersWithPaginationAndFilter(Pageable pageable, LocalDateTime from, LocalDateTime to, List<String> statuses) {
        Page<Order> orders = orderRepository.findAll(OrderSpecifications.withCreationDateRangeAndStatuses(from, to, statuses), pageable);
        return orders.map(order -> {
            UserDto user = getUserById(order.getUserId());
            return new OrderWithUserDto(orderMapper.toDto(order), user);
        });
    }

    @Transactional
    public List<OrderWithUserDto> getOrdersByUserEmail(String email) {
        UserDto user = getUserByEmail(email);
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(user.getId());
        return orders.stream()
                .map(order -> new OrderWithUserDto(orderMapper.toDto(order), user))
                .toList();
    }

    @Transactional
    public OrderWithUserDto updateOrderById(Long id, UpdateOrderDto updatedDto) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        Long newUserId = order.getUserId();
        if (updatedDto.getUserEmail() != null) {
            UserDto user = getUserByEmail(updatedDto.getUserEmail());
            newUserId = user.getId();
        }

        if (updatedDto.getStatus() != null) {
            order.setStatus(updatedDto.getStatus());
        }

        if (updatedDto.getOrderItems() != null) {
            order.getOrderItems().clear();
            for (CreateOrderItemDto itemDto : updatedDto.getOrderItems()) {
                OrderItem item = orderItemCreateMapper.toEntity(itemDto);
                Item fullItem = itemRepository.findById(item.getItem().getId())
                        .orElseThrow(() -> new ItemNotFoundException(item.getItem().getId()));
                item.setItem(fullItem);
                item.setOrder(order);
                order.getOrderItems().add(item);
            }
        }

        order.setUserId(newUserId);
        order.calculateTotalPrice();

        Order saved = orderRepository.save(order);
        UserDto user = getUserById(3L);
        return new OrderWithUserDto(orderMapper.toDto(saved), user);
    }

    @Transactional
    public void deleteOrderById(Long id) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setDeleted(true);
            orderRepository.save(order);
        });
    }

    private UserDto getUserById(Long id) {
        return userService.getUserById(id).get();
    }

    private UserDto getUserByEmail(String email) {
        return userService.getUserByEmail(email).get();
    }
}