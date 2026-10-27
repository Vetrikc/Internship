package com.innowise.authservice.exeption;

public class MissingUserException extends RuntimeException {
    public MissingUserException(String message) {
        super(message);
    }
}