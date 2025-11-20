package com.innowise.internship.integration;

import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.entity.PaymentCard;
import com.innowise.internship.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

@Testcontainers
@SpringBootTest
public class RepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("--save", "", "--appendonly", "no")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @BeforeEach
    void setUp() {
        // Clean up databases before each test to ensure isolation
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSaveAndRetrieveUserWithPaymentCards() {
        // Create and save a User with PaymentCards
        User user = new User();
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmail("john@example.com");
        user.setActive(true);

        PaymentCard card1 = new PaymentCard();
        card1.setNumber("1234567890123456");
        card1.setHolder("John Doe");
        card1.setExpirationDate(LocalDate.of(2025, 12, 31));
        card1.setActive(true);
        card1.setUser(user);

        PaymentCard card2 = new PaymentCard();
        card2.setNumber("6543210987654321");
        card2.setHolder("John Doe");
        card2.setExpirationDate(LocalDate.of(2026, 12, 31));
        card2.setActive(true);
        card2.setUser(user);

        user.getPaymentCards().add(card1);
        user.getPaymentCards().add(card2);

        User savedUser = userRepository.save(user);

        // Verify retrieval via PaymentCardRepository
        List<PaymentCard> cards = paymentCardRepository.findByUserId(savedUser.getId());
        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).getNumber()).isEqualTo("1234567890123456");
        assertThat(cards.get(1).getNumber()).isEqualTo("6543210987654321");

        long count = paymentCardRepository.countByUserId(savedUser.getId());
        assertThat(count).isEqualTo(2);

        // Verify User retrieval
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertThat(retrievedUser.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testFindAllWithSpecificationAndEntityGraph() {
        // Save two users with cards
        User user1 = new User();
        user1.setName("Alice");
        user1.setSurname("Smith");
        user1.setBirthDate(LocalDate.of(1985, 5, 15));
        user1.setEmail("alice@example.com");
        user1.setActive(true);

        PaymentCard card1 = new PaymentCard();
        card1.setNumber("1111222233334444");
        card1.setHolder("Alice Smith");
        card1.setExpirationDate(LocalDate.of(2024, 10, 31));
        card1.setActive(true);
        card1.setUser(user1);
        user1.getPaymentCards().add(card1);

        userRepository.save(user1);

        User user2 = new User();
        user2.setName("Bob");
        user2.setSurname("Johnson");
        user2.setBirthDate(LocalDate.of(1995, 3, 20));
        user2.setEmail("bob@example.com");
        user2.setActive(false);

        PaymentCard card2 = new PaymentCard();
        card2.setNumber("5555666677778888");
        card2.setHolder("Bob Johnson");
        card2.setExpirationDate(LocalDate.of(2027, 1, 31));
        card2.setActive(false);
        card2.setUser(user2);
        user2.getPaymentCards().add(card2);

        userRepository.save(user2);

        // Define a Specification to filter active users
        Specification<User> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> usersPage = userRepository.findAll(activeSpec, pageable);

        assertThat(usersPage.getContent()).hasSize(1);
        User foundUser = usersPage.getContent().get(0);
        assertThat(foundUser.getEmail()).isEqualTo("alice@example.com");

        // Verify EntityGraph loads paymentCards eagerly
        assertThat(foundUser.getPaymentCards()).hasSize(1);
        assertThat(foundUser.getPaymentCards().get(0).getNumber()).isEqualTo("1111222233334444");
    }

    @Test
    void testCountByUserIdWithNoCards() {
        User user = new User();
        user.setName("NoCard");
        user.setSurname("User");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setEmail("nocard@example.com");
        user.setActive(true);

        User savedUser = userRepository.save(user);

        long count = paymentCardRepository.countByUserId(savedUser.getId());
        assertThat(count).isEqualTo(0);

        List<PaymentCard> cards = paymentCardRepository.findByUserId(savedUser.getId());
        assertThat(cards).isEmpty();
    }
}