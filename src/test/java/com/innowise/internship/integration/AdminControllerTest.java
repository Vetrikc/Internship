package com.innowise.internship.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.service.CustomUserDetailsService;
import com.innowise.internship.service.PaymentCardService;
import com.innowise.internship.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AdminControllerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("password");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("--save", "", "--appendonly", "no")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    @Container
    static final PostgreSQLContainer<?> authPostgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("auth_testdb")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.main.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.main.username", postgres::getUsername);
        registry.add("spring.datasource.main.password", postgres::getPassword);
        registry.add("spring.datasource.main.hikari.jdbcUrl", postgres::getJdbcUrl);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.datasource.auth.jdbc-url", authPostgres::getJdbcUrl);
        registry.add("spring.datasource.auth.username", authPostgres::getUsername);
        registry.add("spring.datasource.auth.password", authPostgres::getPassword);
        registry.add("spring.datasource.auth.hikari.jdbcUrl", authPostgres::getJdbcUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PaymentCardService paymentCardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication createAdminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void activateUser_Success() throws Exception {
        Long userId = 1L;
        when(customUserDetailsService.activateUser(userId)).thenReturn(null);

        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/user/" + userId + "/activate")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateUser_Success() throws Exception {
        Long userId = 1L;
        when(customUserDetailsService.deactivateUser(userId)).thenReturn(null);

        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/user/" + userId + "/deactivate")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserDto> users = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(userService.getAllUsers(any(Pageable.class), eq(null), eq(null))).thenReturn(users);

        Authentication auth = createAdminAuth();

        mockMvc.perform(get("/api/v1/admin/user")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_Success() throws Exception {
        Long userId = 1L;
        UserDto userDto = new UserDto();

        when(userService.getUserById(userId)).thenReturn(Optional.of(userDto));

        Authentication auth = createAdminAuth();

        mockMvc.perform(get("/api/v1/admin/user/" + userId)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getCardById_Success() throws Exception {
        Long cardId = 1L;
        PaymentCardDto cardDto = new PaymentCardDto();

        when(paymentCardService.getCardById(cardId)).thenReturn(cardDto);

        Authentication auth = createAdminAuth();

        mockMvc.perform(get("/api/v1/admin/cards/" + cardId)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCards_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCardDto> cards = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(paymentCardService.getAllCards(any(Pageable.class))).thenReturn(cards);

        Authentication auth = createAdminAuth();

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getCardsByUserId_Success() throws Exception {
        Long userId = 1L;
        List<PaymentCardDto> cards = Collections.singletonList(new PaymentCardDto());

        when(paymentCardService.getCardsByUserId(userId)).thenReturn(cards);

        Authentication auth = createAdminAuth();

        mockMvc.perform(get("/api/v1/admin/cards/users/" + userId)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCard_Success() throws Exception {
        Long cardId = 1L;
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("yguyguiy");
        PaymentCardDto updatedCard = new PaymentCardDto();

        when(paymentCardService.updateCard(eq(cardId), any(PaymentCardDto.class))).thenReturn(updatedCard);

        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/cards/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto))
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void activateCard_Success() throws Exception {
        Long cardId = 1L;
        when(paymentCardService.activateCard(cardId)).thenReturn(null);

        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/cards/" + cardId + "/activate")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateCard_Success() throws Exception {
        Long cardId = 1L;
        when(paymentCardService.deactivateCard(cardId)).thenReturn(null);

        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/cards/" + cardId + "/deactivate")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void setAdmin_Success() throws Exception {
        Long userId = 1L;
        when(customUserDetailsService.setAdminRole(userId)).thenReturn(null);
        Authentication auth = createAdminAuth();

        mockMvc.perform(put("/api/v1/admin/user/" + userId + "/admin")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/user"))
                .andExpect(status().isUnauthorized());
    }
}