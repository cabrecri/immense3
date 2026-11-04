package com.example.peopleapi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PeopleApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAll_returnsSeededPeopleFromDatabase() throws Exception {
        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].email").value(
                        containsInAnyOrder("ada@example.com", "alan@example.com", "grace@example.com")));
    }

    @Test
    void getOne_returnsSeededPerson() throws Exception {
        mockMvc.perform(get("/api/people/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1815-12-10"));
    }

    @Test
    void getOne_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/people/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Person not found"))
                .andExpect(jsonPath("$.detail").value("Person with id 999 not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/people/999"))
                .andExpect(jsonPath("$.type").doesNotExist());
    }
}
