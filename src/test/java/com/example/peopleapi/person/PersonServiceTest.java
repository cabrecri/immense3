package com.example.peopleapi.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    @Test
    void findPage_requestsZeroBasedPageSortedByIdAndMapsResult() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ada, alan), PageRequest.of(0, 2), 3));

        PagedPeopleResponse result = service.findPage(2, 1);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("id"));

        assertThat(result.content()).containsExactly(
                new PersonResponse(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10)),
                new PersonResponse(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23)));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.totalItems()).isEqualTo(3L);
    }

    @Test
    void findPage_secondPageUsesIndexOne() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 2), 3));

        PagedPeopleResponse result = service.findPage(2, 2);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalItems()).isEqualTo(3L);
    }

    @Test
    void findPage_rejectsSizeBelowOne() {
        assertThatThrownBy(() -> service.findPage(0, 1))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("n must be >= 1 but was 0");
        verifyNoInteractions(repository);
    }

    @Test
    void findPage_rejectsPageBelowOne() {
        assertThatThrownBy(() -> service.findPage(2, 0))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("numpage must be >= 1 but was 0");
        verifyNoInteractions(repository);
    }

    @Test
    void findPage_rejectsSizeAboveMax() {
        assertThatThrownBy(() -> service.findPage(101, 1))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("n must be <= 100 but was 101");
        verifyNoInteractions(repository);
    }

    @Test
    void findPage_acceptsSizeAtMax() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 3));

        PagedPeopleResponse result = service.findPage(100, 1);

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void findPage_rejectsOffsetBeyondIntegerRange() {
        assertThatThrownBy(() -> service.findPage(100, 30000000))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("numpage is too large for n=100");
        verifyNoInteractions(repository);
    }

    @Test
    void findPage_acceptsLargestPageWithinIntegerRange() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 3));

        PagedPeopleResponse result = service.findPage(1, Integer.MAX_VALUE);

        assertThat(result.content()).isEmpty();
    }
}
