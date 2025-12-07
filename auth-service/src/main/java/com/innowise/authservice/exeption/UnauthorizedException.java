package com.innowise.authservice.exeption;

public class UnauthorizedException extends Throwable {
    public UnauthorizedException(String string) {
        super(string);
    }
}
