package com.example.peopleapi.security;

public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException() {
        super("Missing X-API-Key header");
    }
}
