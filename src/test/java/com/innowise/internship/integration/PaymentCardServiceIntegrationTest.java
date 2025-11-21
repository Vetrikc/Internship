package com.innowise.internship.integration;

import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entitiy.PaymentCard;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.exception.CardLimitExceededException;
import com.innowise.internship.exception.CardNotFoundException;
import com.innowise.internship.exception.InvalidRequestException;
import com.innowise.internship.exception.MissingUserException;
import com.innowise.internship.mapper.PaymentCardMapper;
import com.innowise.internship.service.PaymentCardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentCardServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private PaymentCardService paymentCardService;

    @Autowired
    private PaymentCardMapper paymentCardMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private User testUser;

    @Autowired
    CacheManager cacheManager;

    private Cache usersCache;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up databases before each test
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("testuser@example.com"); // Add this to satisfy the NOT NULL constraint
        // If surname or other fields are also required, set them here (e.g., testUser.setSurname("Test"));
        userRepository.save(testUser);

        usersCache = cacheManager.getCache("users");
        usersCache.clear();
    }

    @AfterEach
    void tearDown() {
        usersCache.clear();
    }

    @Test
    void createCardSuccess() {
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setUserId(testUser.getId());
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("Test Holder");
        cardDto.setExpirationDate(LocalDate.of(2025, 12, 31));
        cardDto.setActive(true);

        PaymentCardDto created = paymentCardService.createCard(cardDto);

        assertEquals(cardDto.getNumber(), created.getNumber());
        assertEquals(testUser.getId(), created.getUserId());

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(testUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

    @Test
    void createCardMissingUserIdThrowsMissingUserException() {
        PaymentCardDto cardDto = new PaymentCardDto();
        // No userId set

        assertThrows(MissingUserException.class, () -> paymentCardService.createCard(cardDto));
    }

    @Test
    void createCardInvalidUserThrowsInvalidRequestException() {
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setUserId(999L); // Non-existent user

        assertThrows(InvalidRequestException.class, () -> paymentCardService.createCard(cardDto));
    }

    @Test
    void createCardExceedsLimitThrowsCardLimitExceededException() {
        // Create 5 cards
        for (int i = 0; i < 5; i++) {
            PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setUserId(testUser.getId());
            cardDto.setNumber("card" + i);
            cardDto.setHolder("Holder");
            cardDto.setExpirationDate(LocalDate.now().plusYears(1));
            paymentCardService.createCard(cardDto);
        }

        // Try to create 6th
        PaymentCardDto extraCard = new PaymentCardDto();
        extraCard.setUserId(testUser.getId());
        extraCard.setNumber("extra");
        extraCard.setHolder("Holder");
        extraCard.setExpirationDate(LocalDate.now().plusYears(1));

        assertThrows(CardLimitExceededException.class, () -> paymentCardService.createCard(extraCard));
    }

    @Test
    void getCardByIdSuccess() {
        PaymentCard card = new PaymentCard();
        card.setUser(testUser);
        card.setNumber("1234");
        card.setHolder("Holder");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);

        Optional<PaymentCardDto> found = paymentCardService.getCardById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getNumber(), found.get().getNumber());
    }

    @Test
    void getCardByIdNotFoundReturnsEmpty() {
        Optional<PaymentCardDto> found = paymentCardService.getCardById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void getAllCardsSuccess() {
        // Create two cards
        for (int i = 0; i < 2; i++) {
            PaymentCard card = new PaymentCard();
            card.setUser(testUser);
            card.setNumber("card" + i);
            card.setHolder("Holder");
            card.setExpirationDate(LocalDate.now().plusYears(1));
            card.setActive(true);
            paymentCardRepository.save(card);
        }

        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCardDto> page = paymentCardService.getAllCards(pageable);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void getCardsByUserIdSuccess() {
        // Create cards for testUser
        for (int i = 0; i < 3; i++) {
            PaymentCard card = new PaymentCard();
            card.setUser(testUser);
            card.setNumber("card" + i);
            card.setHolder("Holder");
            card.setExpirationDate(LocalDate.now().plusYears(1));
            card.setActive(true);
            paymentCardRepository.save(card);
        }

        List<PaymentCardDto> cards = paymentCardService.getCardsByUserId(testUser.getId());

        assertEquals(3, cards.size());
    }

    @Test
    void updateCardSuccess() {
        PaymentCard card = new PaymentCard();
        card.setUser(testUser);
        card.setNumber("old");
        card.setHolder("old");
        card.setExpirationDate(LocalDate.now());
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);

        PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setNumber("new");
        updateDto.setHolder("new");
        updateDto.setExpirationDate(LocalDate.now().plusYears(1));

        PaymentCardDto updated = paymentCardService.updateCard(saved.getId(), updateDto);

        assertEquals("new", updated.getNumber());
        assertEquals("new", updated.getHolder());

        PaymentCard fromDb = paymentCardRepository.findById(saved.getId()).get();
        assertEquals("new", fromDb.getNumber());

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(testUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

    @Test
    void updateCardNotFoundThrowsCardNotFoundException() {
        PaymentCardDto updateDto = new PaymentCardDto();

        assertThrows(CardNotFoundException.class, () -> paymentCardService.updateCard(999L, updateDto));
    }

    @Test
    void deleteCardSuccess() {
        PaymentCard card = new PaymentCard();
        card.setUser(testUser);
        card.setNumber("toDelete");
        card.setHolder("Holder");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);

        PaymentCardDto deleted = paymentCardService.deleteCard(saved.getId());

        assertEquals(saved.getNumber(), deleted.getNumber());

        assertFalse(paymentCardRepository.findById(saved.getId()).isPresent());

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(testUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

    @Test
    void deleteCardNotFoundThrowsCardNotFoundException() {
        assertThrows(CardNotFoundException.class, () -> paymentCardService.deleteCard(999L));
    }

    @Test
    void activateCardSuccess() {
        PaymentCard card = new PaymentCard();
        card.setUser(testUser);
        card.setNumber("inactive");
        card.setHolder("Holder");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setActive(false);
        PaymentCard saved = paymentCardRepository.save(card);

        PaymentCardDto activated = paymentCardService.activateCard(saved.getId());

        assertTrue(activated.getActive());

        PaymentCard fromDb = paymentCardRepository.findById(saved.getId()).get();
        assertTrue(fromDb.getActive());

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(testUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

    @Test
    void activateCardNotFoundThrowsCardNotFoundException() {
        assertThrows(CardNotFoundException.class, () -> paymentCardService.activateCard(999L));
    }

    @Test
    void deactivateCardSuccess() {
        PaymentCard card = new PaymentCard();
        card.setUser(testUser);
        card.setNumber("active");
        card.setHolder("Holder");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);

        PaymentCardDto deactivated = paymentCardService.deactivateCard(saved.getId());

        assertFalse(deactivated.getActive());

        PaymentCard fromDb = paymentCardRepository.findById(saved.getId()).get();
        assertFalse(fromDb.getActive());

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(testUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

    @Test
    void deactivateCardNotFoundThrowsCardNotFoundException() {
        assertThrows(CardNotFoundException.class, () -> paymentCardService.deactivateCard(999L));
    }
}