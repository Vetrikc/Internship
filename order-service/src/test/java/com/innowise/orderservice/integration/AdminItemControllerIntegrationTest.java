package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithMockUser(username = "admin", roles = "ADMIN") // Все тесты от лица админа
class AdminItemControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0")
                    .asCompatibleSubstituteFor("apache/kafka")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        kafka.start();
        registry.add("spring.datasource.order.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.order.username", postgres::getUsername);
        registry.add("spring.datasource.order.password", postgres::getPassword);
        registry.add("spring.datasource.order.hikari.jdbcUrl", postgres::getJdbcUrl);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");

    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    private Item createAndSaveItem(String name, BigDecimal price, int stock) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    private ItemDto sampleItemDto() {
        ItemDto dto = new ItemDto();
        dto.setName("Test Item");
        dto.setPrice(BigDecimal.valueOf(99.99));
        return dto;
    }

    @BeforeEach
    @Transactional
    void setUp() {
        itemRepository.deleteAll();
    }



    @Test
    void createItem_ShouldReturn201_AndCreatedItem() throws Exception {
        ItemDto dto = sampleItemDto();

        String response = mockMvc.perform(post("/api/v1/admin/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andReturn().getResponse().getContentAsString();

        ItemDto created = objectMapper.readValue(response, ItemDto.class);

        assertThat(created.getId()).isNotNull();
        assertThat(itemRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void createItem_InvalidData_ShouldReturn400() throws Exception {
        ItemDto invalid = new ItemDto();
        invalid.setName(""); // @NotBlank
        invalid.setPrice(BigDecimal.valueOf(-50)); // @PositiveOrZero или @Positive

        mockMvc.perform(post("/api/v1/admin/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllItems_WithNameFilter_ShouldReturnFilteredItems() throws Exception {
        createAndSaveItem("Apple iPhone 15", BigDecimal.TEN, 10);
        createAndSaveItem("Samsung Galaxy S24", BigDecimal.TEN, 10);
        createAndSaveItem("Apple MacBook Pro", BigDecimal.TEN, 10);

        mockMvc.perform(get("/api/v1/admin/items")
                        .param("name", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Apple iPhone 15"))
                .andExpect(jsonPath("$.content[1].name").value("Apple MacBook Pro"));
    }

    @Test
    void getItemById_Existing_ShouldReturn200() throws Exception {
        Item saved = createAndSaveItem("Special Item", BigDecimal.valueOf(999.99), 5);

        mockMvc.perform(get("/api/v1/admin/items/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Special Item"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    void getItemById_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/items/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_ShouldReturn200_AndUpdatedData() throws Exception {
        Item saved = createAndSaveItem("Old Name", BigDecimal.valueOf(100), 10);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("New Super Item");
        updateDto.setPrice(BigDecimal.valueOf(299.99));

        mockMvc.perform(put("/api/v1/admin/items/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Super Item"))
                .andExpect(jsonPath("$.price").value(299.99));
    }

    @Test
    void deleteItem_ShouldReturn204_AndRemoveFromDb() throws Exception {
        Item saved = createAndSaveItem("To Be Deleted", BigDecimal.valueOf(1), 1);

        mockMvc.perform(delete("/api/v1/admin/items/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.findById(saved.getId())).isEmpty();
    }
}