package com.innowise.userservice.integration.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.dto.UserProfileCreateRequest;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class InternalUserControllerIntegrationTest {

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
        registry.add("spring.datasource.main.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.main.username", postgres::getUsername);
        registry.add("spring.datasource.main.password", postgres::getPassword);
        registry.add("spring.datasource.main.hikari.jdbcUrl", postgres::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        redis.start();
    }

    @Test
    void createUserProfile_shouldReturn200AndCreatedUser() throws Exception {
        UserProfileCreateRequest request = new UserProfileCreateRequest();
        request.setId(100L);
        request.setEmail("john.doe@example.com");
        request.setName("John");
        request.setSurname("Doe");
        request.setBirthDate(LocalDate.of(1990, 5, 15));

        UserDto returnedFromService = new UserDto();
        returnedFromService.setId(100L);
        returnedFromService.setEmail("john.doe@example.com");
        returnedFromService.setName("John");
        returnedFromService.setSurname("Doe");
        returnedFromService.setBirthDate(LocalDate.of(1990, 5, 15));

        when(userService.createUser(any(UserDto.class))).thenReturn(returnedFromService);

        mockMvc.perform(post("/api/v1/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.name", is("John")))
                .andExpect(jsonPath("$.surname", is("Doe")))
                .andExpect(jsonPath("$.birthDate", is("1990-05-15")));
    }

    @Test
    void getUserByEmail_shouldReturn200AndUser_whenUserExists() throws Exception {
        String email = "jane.doe@example.com";

        UserDto userDto = new UserDto();
        userDto.setId(200L);
        userDto.setEmail(email);
        userDto.setName("Jane");
        userDto.setSurname("Doe");
        userDto.setBirthDate(LocalDate.of(1985, 8, 20));

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(userDto));

        mockMvc.perform(get("/api/v1/internal/by-email/{email}", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.name", is("Jane")))
                .andExpect(jsonPath("$.surname", is("Doe")))
                .andExpect(jsonPath("$.birthDate", is("1985-08-20")));
    }

    @Test
    void getUserByEmail_shouldReturn500_whenUserNotFound() throws Exception {
        String email = "notfound@example.com";

        when(userService.getUserByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/internal/by-email/{email}", email))
                .andExpect(status().isInternalServerError());
    }
}