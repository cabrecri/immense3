package com.example.peopleapi.person;

import java.time.LocalDate;

public record PersonResponse(Long id, String firstName, String lastName, String email, LocalDate birthDate) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getBirthDate());
    }
}
