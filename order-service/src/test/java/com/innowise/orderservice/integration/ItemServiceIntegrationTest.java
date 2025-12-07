package com.innowise.orderservice.integration;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.service.ItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@Rollback
class ItemServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.order.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.order.username", postgres::getUsername);
        registry.add("spring.datasource.order.password", postgres::getPassword);
    }

    @Autowired private ItemService itemService;
    @Autowired private ItemRepository itemRepository;

    private Item createItemEntity(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private ItemDto createItemDto(String name, BigDecimal price) {
        ItemDto dto = new ItemDto();
        dto.setName(name);
        dto.setPrice(price);
        return dto;
    }

    @Test
    void createItem_ShouldPersistAndReturnDtoWithId() {
        ItemDto dto = createItemDto("MacBook Pro", new BigDecimal("2499.99"));

        ItemDto created = itemService.createItem(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("MacBook Pro");
        assertThat(created.getPrice()).isEqualByComparingTo(new BigDecimal("2499.99"));

        assertThat(itemRepository.count()).isEqualTo(1L);
    }

    @Test
    void getItemById_WhenExists_ShouldReturnDto() {
        Item entity = createItemEntity("Wireless Mouse", new BigDecimal("59.99"));
        Item saved = itemRepository.save(entity);

        Optional<ItemDto> found = itemService.getItemById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Wireless Mouse");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("59.99"));
    }

    @Test
    void getItemById_WhenNotExists_ShouldReturnEmpty() {
        assertThat(itemService.getItemById(999L)).isEmpty();
    }

    @Test
    void getAllItems_WithFilter_ShouldReturnMatchingItems() {
        itemRepository.save(createItemEntity("iPhone 15", new BigDecimal("1199")));
        itemRepository.save(createItemEntity("iPhone 14", new BigDecimal("999")));
        itemRepository.save(createItemEntity("Samsung Galaxy", new BigDecimal("899")));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemDto> result = itemService.getAllItems(pageable, "iphone");

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent())
                .extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("iPhone 15", "iPhone 14");
    }

    @Test
    void getAllItems_WhenNoMatch_ShouldReturnEmptyPage() {
        itemRepository.save(createItemEntity("Keyboard", new BigDecimal("129.99")));

        Page<ItemDto> result = itemService.getAllItems(PageRequest.of(0, 10), "nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void updateItem_ShouldUpdateOnlyProvidedFields() {
        Item entity = createItemEntity("Old Laptop", new BigDecimal("800.00"));
        Item saved = itemRepository.save(entity);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("New Laptop Ultra");
        // price не устанавливаем → должен остаться старый

        ItemDto updated = itemService.updateItem(saved.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("New Laptop Ultra");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("800.00"));

        Item fromDb = itemRepository.findById(saved.getId()).get();
        assertThat(fromDb.getName()).isEqualTo("New Laptop Ultra");
        assertThat(fromDb.getPrice()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void updateItem_WithBothFields_ShouldUpdateBoth() {
        Item entity = createItemEntity("Tablet", new BigDecimal("300"));
        Item saved = itemRepository.save(entity);

        ItemDto updateDto = createItemDto("iPad Pro", new BigDecimal("1299.99"));

        ItemDto updated = itemService.updateItem(saved.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("iPad Pro");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    void updateItem_WhenNotFound_ShouldThrowException() {
        ItemDto dto = createItemDto("Doesn't matter", new BigDecimal("1"));

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> itemService.updateItem(999L, dto))
                .withMessageContaining("999");
    }

    @Test
    void deleteItem_ShouldRemoveEntity() {
        Item entity = createItemEntity("To Delete", new BigDecimal("1"));
        Item saved = itemRepository.save(entity);

        itemService.deleteItem(saved.getId());

        assertThat(itemRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void deleteItem_WhenNotExists_ShouldNotThrow() {
        assertThatNoException().isThrownBy(() -> itemService.deleteItem(999L));
    }
}