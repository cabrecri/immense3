package com.example.peopleapi.person;

public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(Long id) {
        super("Person with id " + id + " not found");
    }
}
