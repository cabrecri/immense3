package com.example.peopleapi.person;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public List<PersonResponse> findAll() {
        return repository.findAll().stream()
                .map(PersonResponse::from)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return repository.findById(id)
                .map(PersonResponse::from)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }
}
