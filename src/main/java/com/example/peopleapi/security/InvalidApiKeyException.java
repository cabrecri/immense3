package com.example.peopleapi.security;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("The provided API key is not valid");
    }
}
