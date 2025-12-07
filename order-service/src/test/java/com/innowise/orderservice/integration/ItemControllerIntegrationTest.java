package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.security.JwtPrincipal;
import com.innowise.orderservice.service.ItemService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ItemControllerIntegrationTest {

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
    private ItemRepository itemRepository;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
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
    void getAllItems_Success() throws Exception {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String name = "test";
        Page<ItemDto> itemsPage = new PageImpl<>(List.of(new ItemDto()));

        when(itemService.getAllItems(eq(pageable), eq(name))).thenReturn(itemsPage);

        mockMvc.perform(get("/api/v1/items")
                        .param("page", "0")
                        .param("size", "10")
                        .param("name", name)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isOk());
    }

    @Test
    void getItemById_Success() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;
        ItemDto itemDto = new ItemDto();
        itemDto.setId(itemId);

        when(itemService.getItemById(itemId)).thenReturn(Optional.of(itemDto));

        mockMvc.perform(get("/api/v1/items/" + itemId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isOk());
    }

    @Test
    void getItemById_NotFound() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;

        when(itemService.getItemById(itemId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/items/" + itemId)
                        .with(authentication(createAuth(userId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isUnauthorized());
    }
}