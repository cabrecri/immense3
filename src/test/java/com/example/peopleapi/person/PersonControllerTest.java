package com.example.peopleapi.person;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.peopleapi.security.ApiKeyFilter;

@WebMvcTest(controllers = PersonController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonService service;

    private final PersonResponse ada = new PersonResponse(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10));
    private final PersonResponse alan = new PersonResponse(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23));

    @Test
    void getAll_returnsListOfPeople() throws Exception {
        when(service.findAll()).thenReturn(List.of(ada, alan));

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ada"))
                .andExpect(jsonPath("$[0].lastName").value("Lovelace"))
                .andExpect(jsonPath("$[0].email").value("ada@example.com"))
                .andExpect(jsonPath("$[0].birthDate").value("1815-12-10"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].firstName").value("Alan"));
    }

    @Test
    void getAll_returnsEmptyArrayWhenNoPeople() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getOne_returnsPersonWhenExists() throws Exception {
        when(service.findById(1L)).thenReturn(ada);

        mockMvc.perform(get("/api/people/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1815-12-10"));
    }

    @Test
    void getOne_returns404ProblemDetailWhenMissing() throws Exception {
        when(service.findById(999L)).thenThrow(new PersonNotFoundException(999L));

        mockMvc.perform(get("/api/people/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Person not found"))
                .andExpect(jsonPath("$.detail").value("Person with id 999 not found"));
    }

    @Test
    void getOne_returns400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/people/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPage_returnsPagedResponse() throws Exception {
        when(service.findPage(2, 1)).thenReturn(new PagedPeopleResponse(List.of(ada, alan), 1, 2, 2, 3L));

        mockMvc.perform(get("/api/people/page").param("n", "2").param("numpage", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"))
                .andExpect(jsonPath("$.content[1].firstName").value("Alan"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void getPage_returns400ProblemDetailWhenParamsAreInvalid() throws Exception {
        when(service.findPage(0, 1)).thenThrow(new InvalidPageRequestException("n must be >= 1 but was 0"));

        mockMvc.perform(get("/api/people/page").param("n", "0").param("numpage", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid page request"))
                .andExpect(jsonPath("$.detail").value("n must be >= 1 but was 0"));
    }

    @Test
    void getPage_returns400WhenParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/people/page").param("n", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPage_returns400WhenParamIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/people/page").param("n", "abc").param("numpage", "1"))
                .andExpect(status().isBadRequest());
    }
}
