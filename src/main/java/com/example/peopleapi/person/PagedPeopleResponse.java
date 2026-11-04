package com.example.peopleapi.person;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagedPeopleResponse(
        List<PersonResponse> content,
        int page,
        int size,
        int totalPages,
        long totalItems) {

    public static PagedPeopleResponse from(Page<Person> page, int requestedPage, int requestedSize) {
        return new PagedPeopleResponse(
                page.getContent().stream().map(PersonResponse::from).toList(),
                requestedPage,
                requestedSize,
                page.getTotalPages(),
                page.getTotalElements());
    }
}
