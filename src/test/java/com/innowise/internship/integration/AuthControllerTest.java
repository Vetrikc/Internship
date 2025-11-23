package com.innowise.internship.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.internship.dto.security.AuthRequest;
import com.innowise.internship.dto.security.RefreshRequest;
import com.innowise.internship.dto.security.RegisterRequest;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.Role;
import com.innowise.internship.security.JwtUtils;
import com.innowise.internship.service.CustomUserDetailsService;
import com.innowise.internship.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

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
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_Success() throws Exception {
        String email = "test@example.com";
        String password = "password";
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);

        AuthUser authUser = new AuthUser();
        authUser.setId(1L);
        authUser.setEmail(email);
        authUser.setRole(Role.USER);
        authUser.setActive(true);

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(authUser);
        when(jwtUtils.generateAccessToken(authUser)).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(authUser)).thenReturn("refreshToken");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));
    }

    @Test
    void login_BadCredentials() throws Exception {
        String email = "test@example.com";
        String password = "wrong";
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);

        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Неверный email или пароль"));
    }

    @Test
    void register_Success() throws Exception {
        String email = "new@example.com";
        String password = "password";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setName("John");
        request.setSurname("Doe");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        AuthUser authUser = new AuthUser();
        authUser.setId(1L);
        authUser.setEmail(email);
        authUser.setPassword("encoded");
        authUser.setRole(Role.USER);
        authUser.setActive(true);

        when(customUserDetailsService.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded");
        when(customUserDetailsService.createAuthUser(any(AuthUser.class))).thenReturn(authUser);
        when(userService.createUser(any(UserDto.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User has been created"));
    }

    @Test
    void register_EmailExists() throws Exception {
        String email = "existing@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("password");

        when(customUserDetailsService.existsByEmail(email)).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email уже зарегистрирован"));
    }

    @Test
    void refresh_Success() throws Exception {
        String refreshToken = "refreshToken";
        String email = "test@example.com";
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        AuthUser authUser = new AuthUser();
        authUser.setEmail(email);

        when(jwtUtils.extractEmail(refreshToken)).thenReturn(email);
        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(authUser);
        when(jwtUtils.validateToken(eq(refreshToken), eq(authUser))).thenReturn(true);
        when(jwtUtils.generateAccessToken(authUser)).thenReturn("newAccessToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));
    }

    @Test
    void refresh_InvalidToken() throws Exception {
        String refreshToken = "invalidToken";
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        String email = "test@example.com";

        when(jwtUtils.extractEmail(refreshToken)).thenReturn(email);
        when(customUserDetailsService.loadUserByUsername(email)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }
}