package com.example.peopleapi.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository repository;

    @InjectMocks
    private PersonService service;

    private final Person ada = new Person(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10));
    private final Person alan = new Person(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23));

    @Test
    void findAll_mapsEveryPersonToResponse() {
        when(repository.findAll()).thenReturn(List.of(ada, alan));

        List<PersonResponse> result = service.findAll();

        assertThat(result).containsExactly(
                new PersonResponse(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10)),
                new PersonResponse(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23)));
    }

    @Test
    void findAll_returnsEmptyListWhenNoPeople() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findById_returnsResponseWhenExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(ada));

        PersonResponse result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Ada");
        assertThat(result.email()).isEqualTo("ada@example.com");
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessage("Person with id 999 not found");
    }
}
