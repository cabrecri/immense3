package com.example.peopleapi.person;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PersonRepositoryTest {

    @Autowired
    private PersonRepository repository;

    @Test
    void findAll_returnsSeededPeople() {
        List<Person> people = repository.findAll();

        assertThat(people).hasSize(3);
        assertThat(people).extracting(Person::getEmail)
                .containsExactlyInAnyOrder("ada@example.com", "alan@example.com", "grace@example.com");
    }

    @Test
    void findById_returnsPersonWhenExists() {
        Optional<Person> person = repository.findById(1L);

        assertThat(person).isPresent();
        assertThat(person.get().getFirstName()).isEqualTo("Ada");
        assertThat(person.get().getLastName()).isEqualTo("Lovelace");
        assertThat(person.get().getBirthDate()).isEqualTo(LocalDate.of(1815, 12, 10));
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        assertThat(repository.findById(999L)).isEmpty();
    }

    @Test
    void save_assignsGeneratedId() {
        Person saved = repository.save(new Person("Linus", "Torvalds", "linus@example.com", LocalDate.of(1969, 12, 28)));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
