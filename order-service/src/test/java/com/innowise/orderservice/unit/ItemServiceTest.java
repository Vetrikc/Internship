package com.innowise.orderservice.unit;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    private Item item;
    private ItemDto itemDto;

    private static final Long ITEM_ID = 1L;
    private static final String ITEM_NAME = "Laptop";
    private static final BigDecimal ITEM_PRICE = new BigDecimal("999.99");

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(ITEM_ID);
        item.setName(ITEM_NAME);
        item.setPrice(ITEM_PRICE);

        itemDto = new ItemDto();
        itemDto.setId(ITEM_ID);
        itemDto.setName(ITEM_NAME);
        itemDto.setPrice(ITEM_PRICE);
    }

    @Test
    void createItem_ShouldReturnCreatedItemDto() {
        when(itemMapper.toEntity(itemDto)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        ItemDto result = itemService.createItem(itemDto);

        assertNotNull(result);
        assertEquals(ITEM_NAME, result.getName());
        assertEquals(ITEM_PRICE, result.getPrice());

        verify(itemMapper).toEntity(itemDto);
        verify(itemRepository).save(item);
        verify(itemMapper).toDto(item);
    }

    @Test
    void getItemById_WhenItemExists_ShouldReturnOptionalWithItemDto() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        Optional<ItemDto> result = itemService.getItemById(ITEM_ID);

        assertTrue(result.isPresent());
        assertEquals(ITEM_NAME, result.get().getName());
        verify(itemRepository).findById(ITEM_ID);
    }

    @Test
    void getItemById_WhenItemNotExists_ShouldReturnEmptyOptional() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        Optional<ItemDto> result = itemService.getItemById(ITEM_ID);

        assertTrue(result.isEmpty());
        verify(itemRepository).findById(ITEM_ID);
    }

    @Test
    void getAllItems_ShouldReturnPageOfItemDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> itemPage = new PageImpl<>(java.util.List.of(item), pageable, 1);

        when(itemRepository.findByNameContainingIgnoreCase("lap", pageable)).thenReturn(itemPage);
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        Page<ItemDto> result = itemService.getAllItems(pageable, "lap");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(ITEM_NAME, result.getContent().get(0).getName());

        verify(itemRepository).findByNameContainingIgnoreCase("lap", pageable);
    }

    @Test
    void updateItem_WhenItemExists_ShouldUpdateAndReturnItemDto() {
        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Laptop");
        updateDto.setPrice(new BigDecimal("1299.99"));

        Item updatedItem = new Item();
        updatedItem.setId(ITEM_ID);
        updatedItem.setName("Updated Laptop");
        updatedItem.setPrice(new BigDecimal("1299.99"));

        ItemDto updatedItemDto = new ItemDto();
        updatedItemDto.setId(ITEM_ID);
        updatedItemDto.setName("Updated Laptop");
        updatedItemDto.setPrice(new BigDecimal("1299.99"));

        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(updatedItem);
        when(itemMapper.toDto(updatedItem)).thenReturn(updatedItemDto);

        ItemDto result = itemService.updateItem(ITEM_ID, updateDto);

        assertEquals("Updated Laptop", result.getName());
        assertEquals(new BigDecimal("1299.99"), result.getPrice());

        verify(itemRepository).findById(ITEM_ID);
        verify(itemRepository).save(item);
    }

    @Test
    void updateItem_WhenItemNotFound_ShouldThrowItemNotFoundException() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        ItemNotFoundException exception = assertThrows(
            ItemNotFoundException.class,
            () -> itemService.updateItem(ITEM_ID, itemDto)
        );

        assertEquals("Item not found with id: 1", exception.getMessage());
        verify(itemRepository).findById(ITEM_ID);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void deleteItem_ShouldCallRepositoryDeleteById() {
        doNothing().when(itemRepository).deleteById(ITEM_ID);

        itemService.deleteItem(ITEM_ID);

        verify(itemRepository).deleteById(ITEM_ID);
    }

    @Test
    void deleteItem_WhenItemNotExists_ShouldStillCallDeleteById() {
        // deleteById не бросает исключение, если сущность не найдена (в JPA)
        doNothing().when(itemRepository).deleteById(999L);

        itemService.deleteItem(999L);

        verify(itemRepository).deleteById(999L);
    }
}