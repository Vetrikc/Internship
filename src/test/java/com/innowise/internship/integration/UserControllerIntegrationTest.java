package com.innowise.internship.integration;

import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entitiy.User;
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
class UserControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() throws Exception {
        String json = "{\"email\":\"test@example.com\",\"name\":\"John\",\"surname\":\"Doe\",\"birthDate\":\"2000-01-01\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void testGetUserById() throws Exception {
        User entity = new User();
        entity.setEmail("test@example.com");
        entity.setName("John");
        entity.setSurname("Doe");
        entity.setBirthDate(LocalDate.parse("2000-01-01"));
        User saved = userRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers() throws Exception {
        User entity1 = new User();
        entity1.setEmail("user1@example.com");
        entity1.setName("John");
        entity1.setSurname("Doe");
        entity1.setBirthDate(LocalDate.parse("2000-01-01"));
        userRepository.save(entity1);

        User entity2 = new User();
        entity2.setEmail("user2@example.com");
        entity2.setName("Jane");
        entity2.setSurname("Doe");
        entity2.setBirthDate(LocalDate.parse("2001-02-02"));
        userRepository.save(entity2);

        mockMvc.perform(MockMvcRequestBuilders.get("/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
    }

    @Test
    void testGetAllUsersWithFilters() throws Exception {
        User entity1 = new User();
        entity1.setEmail("user1@example.com");
        entity1.setName("John");
        entity1.setSurname("Doe");
        entity1.setBirthDate(LocalDate.parse("2000-01-01"));
        userRepository.save(entity1);

        User entity2 = new User();
        entity2.setEmail("user2@example.com");
        entity2.setName("Jane");
        entity2.setSurname("Smith");
        entity2.setBirthDate(LocalDate.parse("2001-02-02"));
        userRepository.save(entity2);

        mockMvc.perform(MockMvcRequestBuilders.get("/users?page=0&size=10&name=John&surname=Doe"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }

    @Test
    void testUpdateUser() throws Exception {
        User entity = new User();
        entity.setEmail("test@example.com");
        entity.setName("John");
        entity.setSurname("Doe");
        entity.setBirthDate(LocalDate.parse("2000-01-01"));
        User saved = userRepository.save(entity);

        String updatedJson = "{\"email\":\"updated@example.com\",\"name\":\"Updated John\",\"surname\":\"Updated Doe\",\"birthDate\":\"2001-01-01\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("updated@example.com"));

        User updatedEntity = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedEntity.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedEntity.getName()).isEqualTo("Updated John");
        assertThat(updatedEntity.getSurname()).isEqualTo("Updated Doe");
        assertThat(updatedEntity.getBirthDate()).isEqualTo(LocalDate.parse("2001-01-01"));
    }

    @Test
    void testDeleteUser() throws Exception {
        User entity = new User();
        entity.setEmail("test@example.com");
        entity.setName("John");
        entity.setSurname("Doe");
        entity.setBirthDate(LocalDate.parse("2000-01-01"));
        User saved = userRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void testActivateUser() throws Exception {
        User entity = new User();
        entity.setEmail("test@example.com");
        entity.setName("John");
        entity.setSurname("Doe");
        entity.setBirthDate(LocalDate.parse("2000-01-01"));
        entity.setActive(false);
        User saved = userRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + saved.getId() + "/activate"))
                .andExpect(status().isOk());

        User updated = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getActive()).isTrue();
    }

    @Test
    void testDeactivateUser() throws Exception {
        User entity = new User();
        entity.setEmail("test@example.com");
        entity.setName("John");
        entity.setSurname("Doe");
        entity.setBirthDate(LocalDate.parse("2000-01-01"));
        entity.setActive(true);
        User saved = userRepository.save(entity);

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + saved.getId() + "/deactivate"))
                .andExpect(status().isOk());

        User updated = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getActive()).isFalse();
    }
}