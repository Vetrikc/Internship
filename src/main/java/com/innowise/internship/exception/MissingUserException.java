package com.innowise.internship.exception;

public class MissingUserException extends RuntimeException {
    public MissingUserException(String message) {
        super(message);
    }
}