package com.innowise.internship.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.PaymentCard;
import com.innowise.internship.entity.User;
import com.innowise.internship.service.PaymentCardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PaymentCardControllerIntegrationTest {

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

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @MockBean
    private PaymentCardService paymentCardService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        paymentCardRepository.deleteAll();

    }

    @AfterEach
    void tearDown() {
        paymentCardRepository.deleteAll();
    }

    private Authentication createAuth(Long userId) {
        AuthUser principal = Mockito.mock(AuthUser.class);
        when(principal.getId()).thenReturn(userId);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getAllUserCards_Success() throws Exception {
        Long userId = 1L;
        List<PaymentCardDto> cards = Collections.singletonList(new PaymentCardDto());

        when(paymentCardService.getCardsByUserId(userId)).thenReturn(cards);

        Authentication auth = createAuth(userId);

        mockMvc.perform(get("/api/v1/cards")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }
    @Test
    void createCard_Success() throws Exception {
        Long userId = 1L;
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("aa");
        PaymentCardDto createdCard = new PaymentCardDto();



        when(paymentCardService.createCard(any(PaymentCardDto.class))).thenReturn(createdCard);

        Authentication auth = createAuth(userId);

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto))
                        .with(authentication(auth)))
                .andExpect(status().isCreated());
    }

    @Test
    void getCardById_Success() throws Exception {
        Long userId = 1L;
        Long cardId = 1L;
        PaymentCardDto cardDto = new PaymentCardDto();

        when(paymentCardService.getUserCardById(eq(cardId), eq(userId))).thenReturn(cardDto);

        Authentication auth = createAuth(userId);

        mockMvc.perform(get("/api/v1/cards/" + cardId)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCard_Success() throws Exception {
        Long userId = 1L;
        Long cardId = 1L;
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("aa");
        PaymentCardDto updatedCard = new PaymentCardDto();

        when(paymentCardService.updateUserCard(eq(cardId), any(PaymentCardDto.class), eq(userId))).thenReturn(updatedCard);

        Authentication auth = createAuth(userId);

        mockMvc.perform(put("/api/v1/cards/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto))
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCard_Success() throws Exception {
        Long userId = 1L;
        Long cardId = 1L;


        when(paymentCardService.deleteUserCard(eq(cardId), eq(userId))).thenReturn(null);

        Authentication auth = createAuth(userId);

        mockMvc.perform(delete("/api/v1/cards/" + cardId)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUserCards_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isUnauthorized());
    }


}