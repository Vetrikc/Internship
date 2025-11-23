package com.innowise.internship.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entity.User;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
class UserServiceIntegrationTest {

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
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    CacheManager cacheManager;

    private Cache usersCache;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean up before each test
        usersCache = cacheManager.getCache("users");
        usersCache.clear();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll(); // Clean up after each test
        usersCache.clear();
    }


    @Test
    void testCreateUser() {
        // Given
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John");
        userDto.setSurname("Doe");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setEmail("john.doe@example.com");

        // When
        UserDto createdUser = userService.createUser(userDto);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("John");
        assertThat(createdUser.getSurname()).isEqualTo("Doe");

        Optional<User> savedUser = userRepository.findById(createdUser.getId());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("John");

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(createdUser.getId(), UserDto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getName()).isEqualTo("John");
    }

    @Test
    void testGetUserById() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setName("Jane");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1985, 5, 5));
        user.setEmail("jane.smith@example.com");
        User savedUser = userRepository.save(user);

        // When
        Optional<UserDto> foundUser = userService.getUserById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Jane");
        assertThat(foundUser.get().getSurname()).isEqualTo("Smith");

        // Then(caching)
        Optional<UserDto> user1 = userService.getUserById(savedUser.getId());
        Optional<UserDto> user2 = userService.getUserById(savedUser.getId());

        Cache cache = cacheManager.getCache("users");
        assertThat(cache).isNotNull();

        UserDto cached = cache.get(savedUser.getId(), UserDto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getName()).isEqualTo("Jane");

        assertThat(user1).isPresent();
        assertThat(user2).isPresent();
        assertThat(user1.get().getId()).isEqualTo(user2.get().getId());
    }

    @Test
    @Transactional
    void testGetUserByIdNotFound() {
        // When & Then
        Optional<UserDto> foundUser = userService.getUserById(999L);
        assertThat(foundUser).isEmpty();
    }

    @Test
    @Transactional
    void testGetAllUsers() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        user1.setName("Alice");
        user1.setSurname("Johnson");
        user1.setBirthDate(LocalDate.of(1992, 2, 2));
        user1.setEmail("alice@example.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Bob");
        user2.setSurname("Johnson");
        user2.setBirthDate(LocalDate.of(1988, 8, 8));
        user2.setEmail("bob@example.com");
        userRepository.save(user2);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<UserDto> users = userService.getAllUsers(pageable, null, "Johnson");

        // Then
        assertThat(users.getContent()).hasSize(2);
        assertThat(users.getContent().get(0).getName()).isIn("Alice", "Bob");
        assertThat(users.getContent().get(1).getName()).isIn("Alice", "Bob");
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setName("OldName");
        user.setSurname("OldSurname");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setEmail("old@email.com");
        User savedUser = userRepository.save(user);

        UserDto updatedDto = new UserDto();
        updatedDto.setName("NewName");
        updatedDto.setSurname("NewSurname");
        updatedDto.setBirthDate(LocalDate.of(1995, 5, 5));
        updatedDto.setEmail("new@email.com");

        // When
        UserDto updatedUser = userService.updateUser(savedUser.getId(), updatedDto);

        // Then
        assertThat(updatedUser.getName()).isEqualTo("NewName");
        assertThat(updatedUser.getSurname()).isEqualTo("NewSurname");
        assertThat(updatedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 5));

        Optional<User> dbUser = userRepository.findById(savedUser.getId());
        assertThat(dbUser).isPresent();
        assertThat(dbUser.get().getName()).isEqualTo("NewName");

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(savedUser.getId(), UserDto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getName()).isEqualTo("NewName");
    }

    @Test
    @Transactional
    void testUpdateUserNotFound() {
        // Given
        UserDto updatedDto = new UserDto();
        updatedDto.setName("NewName");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, updatedDto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void testDeleteUser() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setName("ToDelete");
        user.setSurname("User");
        user.setBirthDate(LocalDate.now());
        user.setEmail("delete@example.com");
        User savedUser = userRepository.save(user);

        // When
        userService.deleteUser(savedUser.getId());

        // Then
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();

        // Then(caching)
        UserDto cached = cacheManager.getCache("users").get(savedUser.getId(), UserDto.class);
        assertThat(cached).isNull();
    }

}