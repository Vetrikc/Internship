package com.innowise.internship.controller.order;


import com.innowise.internship.dto.order.ItemDto;
import com.innowise.internship.service.order.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/items")
@PreAuthorize("hasRole('USER')")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<Page<ItemDto>> getAllItems(Pageable pageable,
                                                     @RequestParam(required = false) String name) {
        Page<ItemDto> items = itemService.getAllItems(pageable, name);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        Optional<ItemDto> item = itemService.getItemById(id);
        return item.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}