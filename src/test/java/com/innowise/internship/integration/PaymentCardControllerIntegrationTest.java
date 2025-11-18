package com.innowise.internship.integration;

import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entitiy.PaymentCard;
import com.innowise.internship.entitiy.User;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PaymentCardControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("--save", "", "--appendonly", "no")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private UserRepository userRepository; // Assuming UserRepository exists

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateCard() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        String json = String.format("{\"userId\":%d,\"number\":\"1234567890123456\",\"holder\":\"John Doe\",\"expirationDate\":\"2025-12-31\"}", savedUser.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.number").exists());

        assertThat(paymentCardRepository.count()).isEqualTo(1);
    }

    @Test
    void testGetCardById() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        PaymentCard saved = paymentCardRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.get("/cards/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.number").value("1234567890123456"));
    }

    @Test
    void testGetCardByIdNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/cards/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllCards() throws Exception {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        User savedUser1 = userRepository.save(user1);

        PaymentCard entity1 = new PaymentCard();
        entity1.setUser(savedUser1);
        entity1.setNumber("1234567890123456");
        entity1.setHolder("John Doe");
        entity1.setExpirationDate(LocalDate.parse("2025-12-31"));
        paymentCardRepository.save(entity1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        User savedUser2 = userRepository.save(user2);

        PaymentCard entity2 = new PaymentCard();
        entity2.setUser(savedUser2);
        entity2.setNumber("6543210987654321");
        entity2.setHolder("Jane Doe");
        entity2.setExpirationDate(LocalDate.parse("2024-11-30"));
        paymentCardRepository.save(entity2);

        mockMvc.perform(MockMvcRequestBuilders.get("/cards?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
    }

    @Test
    void testGetCardsByUserId() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        paymentCardRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.get("/cards/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].userId").value(savedUser.getId()));
    }

    @Test
    void testUpdateCard() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        PaymentCard saved = paymentCardRepository.save(entity);

        String updatedJson = String.format("{\"userId\":%d,\"number\":\"12345677890123459\",\"holder\":\"Updated Holder\",\"expirationDate\":\"2026-01-01\"}", savedUser.getId());

        mockMvc.perform(MockMvcRequestBuilders.put("/cards/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.number").value("12345677890123459"));

        PaymentCard updatedEntity = paymentCardRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedEntity.getNumber()).isEqualTo("12345677890123459");
        assertThat(updatedEntity.getHolder()).isEqualTo("Updated Holder");
        assertThat(updatedEntity.getExpirationDate()).isEqualTo(LocalDate.parse("2026-01-01"));
    }

    @Test
    void testDeleteCard() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        PaymentCard saved = paymentCardRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.delete("/cards/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(paymentCardRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void testActivateCard() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        entity.setActive(false);
        PaymentCard saved = paymentCardRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.put("/cards/" + saved.getId() + "/activate"))
                .andExpect(status().isOk());

        PaymentCard updated = paymentCardRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getActive()).isTrue();
    }

    @Test
    void testDeactivateCard() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        PaymentCard entity = new PaymentCard();
        entity.setUser(savedUser);
        entity.setNumber("1234567890123456");
        entity.setHolder("John Doe");
        entity.setExpirationDate(LocalDate.parse("2025-12-31"));
        entity.setActive(true);
        PaymentCard saved = paymentCardRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.put("/cards/" + saved.getId() + "/deactivate"))
                .andExpect(status().isOk());

        PaymentCard updated = paymentCardRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getActive()).isFalse();
    }
}