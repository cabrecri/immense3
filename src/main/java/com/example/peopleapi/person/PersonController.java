package com.example.peopleapi.person;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    @GetMapping
    public List<PersonResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PersonResponse getOne(@PathVariable Long id) {
        return service.findById(id);
    }
}
