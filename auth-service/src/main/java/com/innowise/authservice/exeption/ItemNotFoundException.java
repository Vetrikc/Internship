package com.innowise.authservice.exeption;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long id) {
        super("Item not found with id: " + id);
    }
}