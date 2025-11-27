package com.innowise.internship.service.order;


import com.innowise.internship.dao_order.ItemRepository;
import com.innowise.internship.dto.order.ItemDto;
import com.innowise.internship.entity.Item;
import com.innowise.internship.exception.ItemNotFoundException;

import com.innowise.internship.mapper.order.ItemMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public ItemDto createItem(ItemDto itemDto) {
        Item item = itemMapper.toEntity(itemDto);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toDto(savedItem);
    }

    @Transactional
    public Optional<ItemDto> getItemById(Long id) {
        Optional<Item> item = itemRepository.findById(id);
        return item.map(itemMapper::toDto);
    }

    @Transactional
    public Page<ItemDto> getAllItems(Pageable pageable, String name) {
        return itemRepository.findByNameContainingIgnoreCase(name, pageable).map(itemMapper::toDto);
    }

    @Transactional
    public ItemDto updateItem(Long id, ItemDto updatedItemDto) {
        return itemRepository.findById(id).map(item -> {
            if (updatedItemDto.getName() != null) item.setName(updatedItemDto.getName());
            if (updatedItemDto.getPrice() != null) item.setPrice(updatedItemDto.getPrice());
            Item savedItem = itemRepository.save(item);
            return itemMapper.toDto(savedItem);
        }).orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }
}