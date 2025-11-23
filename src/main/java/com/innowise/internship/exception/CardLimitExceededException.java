package com.innowise.internship.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException() {
        super("User cannot have more than 5 cards");
    }
}