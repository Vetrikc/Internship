package com.innowise.internship.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(String message) {
        super(message);
    }
}