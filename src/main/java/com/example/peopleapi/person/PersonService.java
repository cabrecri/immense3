package com.example.peopleapi.person;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonService {

    public static final int MAX_PAGE_SIZE = 100;

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

    public PagedPeopleResponse findPage(int n, int numpage) {
        if (n < 1) {
            throw new InvalidPageRequestException("n must be >= 1 but was " + n);
        }
        if (numpage < 1) {
            throw new InvalidPageRequestException("numpage must be >= 1 but was " + numpage);
        }
        if (n > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException("n must be <= " + MAX_PAGE_SIZE + " but was " + n);
        }
        if ((long) (numpage - 1) * n > Integer.MAX_VALUE) {
            throw new InvalidPageRequestException("numpage is too large for n=" + n);
        }
        Page<Person> page = repository.findAll(PageRequest.of(numpage - 1, n, Sort.by("id")));
        return PagedPeopleResponse.from(page, numpage, n);
    }
}
