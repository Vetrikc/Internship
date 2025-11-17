package com.innowise.internship.integration;

import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.mapper.UserMapper;
import com.innowise.internship.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop"); // Ensure schema is created/dropped
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean up before each test
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll(); // Clean up after each test
    }

    @Test
    @Transactional
    void testCreateUser() {
        // Given
        UserDto userDto = new UserDto();
        userDto.setName("John");
        userDto.setSurname("Doe");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setEmail("john.doe@example.com");
        userDto.setActive(true);

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
    }

    @Test
    @Transactional
    void testGetUserById() {
        // Given
        User user = new User();
        user.setName("Jane");
        user.setSurname("Smith");
        user.setBirthDate(LocalDate.of(1985, 5, 5));
        user.setEmail("jane.smith@example.com");
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // When
        Optional<UserDto> foundUser = userService.getUserById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Jane");
        assertThat(foundUser.get().getSurname()).isEqualTo("Smith");
    }

    @Test
    @Transactional
    void testGetUserById_NotFound() {
        // When & Then
        Optional<UserDto> foundUser = userService.getUserById(999L);
        assertThat(foundUser).isEmpty();
    }

    @Test
    @Transactional
    void testGetAllUsers() {
        // Given
        User user1 = new User();
        user1.setName("Alice");
        user1.setSurname("Johnson");
        user1.setBirthDate(LocalDate.of(1992, 2, 2));
        user1.setEmail("alice@example.com");
        user1.setActive(true);
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("Bob");
        user2.setSurname("Johnson");
        user2.setBirthDate(LocalDate.of(1988, 8, 8));
        user2.setEmail("bob@example.com");
        user2.setActive(true);
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
    @Transactional
    void testUpdateUser() {
        // Given
        User user = new User();
        user.setName("OldName");
        user.setSurname("OldSurname");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setEmail("old@email.com");
        user.setActive(true);
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
        assertThat(updatedUser.getEmail()).isEqualTo("new@email.com");

        Optional<User> dbUser = userRepository.findById(savedUser.getId());
        assertThat(dbUser).isPresent();
        assertThat(dbUser.get().getName()).isEqualTo("NewName");
    }

    @Test
    @Transactional
    void testUpdateUser_NotFound() {
        // Given
        UserDto updatedDto = new UserDto();
        updatedDto.setName("NewName");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, updatedDto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @Transactional
    void testDeleteUser() {
        // Given
        User user = new User();
        user.setName("ToDelete");
        user.setSurname("User");
        user.setBirthDate(LocalDate.now());
        user.setEmail("delete@example.com");
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // When
        userService.deleteUser(savedUser.getId());

        // Then
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @Transactional
    void testActivateUser() {
        // Given
        User user = new User();
        user.setName("Inactive");
        user.setSurname("User");
        user.setBirthDate(LocalDate.now());
        user.setEmail("inactive@example.com");
        user.setActive(false);
        User savedUser = userRepository.save(user);

        // When
        UserDto activatedUser = userService.activateUser(savedUser.getId());

        // Then
        assertThat(activatedUser.getActive()).isTrue();

        Optional<User> dbUser = userRepository.findById(savedUser.getId());
        assertThat(dbUser).isPresent();
        assertThat(dbUser.get().getActive()).isTrue();
    }

    @Test
    @Transactional
    void testActivateUser_NotFound() {
        // When & Then
        assertThatThrownBy(() -> userService.activateUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @Transactional
    void testDeactivateUser() {
        // Given
        User user = new User();
        user.setName("Active");
        user.setSurname("User");
        user.setBirthDate(LocalDate.now());
        user.setEmail("active@example.com");
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // When
        UserDto deactivatedUser = userService.deactivateUser(savedUser.getId());

        // Then
        assertThat(deactivatedUser.getActive()).isFalse();

        Optional<User> dbUser = userRepository.findById(savedUser.getId());
        assertThat(dbUser).isPresent();
        assertThat(dbUser.get().getActive()).isFalse();
    }

    @Test
    @Transactional
    void testDeactivateUser_NotFound() {
        // When & Then
        assertThatThrownBy(() -> userService.deactivateUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }
}