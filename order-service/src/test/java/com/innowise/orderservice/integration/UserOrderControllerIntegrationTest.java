package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.controller.UserServiceClient;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.*;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.security.JwtPrincipal;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserOrderControllerIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.order.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.order.username", postgres::getUsername);
        registry.add("spring.datasource.order.password", postgres::getPassword);
        registry.add("spring.datasource.order.hikari.jdbcUrl", postgres::getJdbcUrl);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ItemRepository itemRepository;
    @MockBean
    private OrderService orderService;
    @MockBean
    private UserServiceClient userServiceClient;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    private Authentication createAuth(Long userId) {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        when(principal.id()).thenReturn(userId);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void createOrder_Success() throws Exception {
        Long userId = 1L;
        CreateOrderItemDto item = new CreateOrderItemDto();
        item.setItemId(1L);

        List<CreateOrderItemDto> list = new ArrayList<>();
        list.add(item);

        CreateOrderDto orderDto = new CreateOrderDto();
        orderDto.setUserId(userId);
        orderDto.setStatus("status");
        orderDto.setOrderItems(list);



        OrderDto createdOrder = new OrderDto();
        createdOrder.setId(1L);
        createdOrder.setStatus("status");
        createdOrder.setOrderItems(new ArrayList<>());

        when(orderService.createOrder(any(CreateOrderDto.class))).thenReturn(createdOrder);
        UserDto userDto = new UserDto();
        userDto.setId(userId);

        when(userServiceClient.getUserByEmail(any(String.class))).thenReturn(userDto);
        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(createAuth(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void getMyOrders_Success() throws Exception {
        Long userId = 1L;
        List<OrderDto> orders = List.of(new OrderDto());
        when(orderService.getOrdersByUserId(userId)).thenReturn(orders);
        UserDto userDto = new UserDto();
        userDto.setId(userId);
        when(userServiceClient.getUserByEmail(any(String.class))).thenReturn(userDto);
        mockMvc.perform(get("/api/v1/orders")
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isOk());
    }

    @Test
    void getMyOrderById_Success() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(userId);
        when(orderService.getOrderById(orderId)).thenReturn(orderDto);
        UserDto userDto = new UserDto();
        userDto.setId(userId);
        when(userServiceClient.getUserByEmail(any(String.class))).thenReturn(userDto);
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isOk());
    }

    @Test
    void getMyOrderById_Forbidden() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(2L); // другой пользователь
        when(orderService.getOrderById(orderId)).thenReturn(orderDto);
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyOrder_Success() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        UpdateOrderDto updateDto = new UpdateOrderDto();
// Заполните DTO
        OrderDto existingOrder = new OrderDto();
        existingOrder.setUserId(userId);
        OrderDto updatedOrder = new OrderDto();
        when(orderService.getOrderById(orderId)).thenReturn(existingOrder);
        when(orderService.updateOrderById(eq(orderId), any(UpdateOrderDto.class))).thenReturn(updatedOrder);
        UserDto userDto = new UserDto();
        userDto.setId(userId);
        when(userServiceClient.getUserByEmail(any(String.class))).thenReturn(userDto);
        mockMvc.perform(put("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateMyOrder_Forbidden() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        UpdateOrderDto updateDto = new UpdateOrderDto();
        OrderDto existingOrder = new OrderDto();
        existingOrder.setUserId(2L);
        when(orderService.getOrderById(orderId)).thenReturn(existingOrder);
        mockMvc.perform(put("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMyOrder_Success() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        OrderDto existingOrder = new OrderDto();
        existingOrder.setUserId(userId);
        when(orderService.getOrderById(orderId)).thenReturn(existingOrder);
        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMyOrder_Forbidden() throws Exception {
        Long userId = 1L;
        Long orderId = 1L;
        OrderDto existingOrder = new OrderDto();
        existingOrder.setUserId(2L);
        when(orderService.getOrderById(orderId)).thenReturn(existingOrder);
        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }
}