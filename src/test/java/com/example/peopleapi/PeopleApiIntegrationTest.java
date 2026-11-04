package com.example.peopleapi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.api-key=test-key")
@AutoConfigureMockMvc
class PeopleApiIntegrationTest {

    private static final String API_KEY = "test-key";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAll_returnsSeededPeopleFromDatabase() throws Exception {
        mockMvc.perform(get("/api/people").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].email").value(
                        containsInAnyOrder("ada@example.com", "alan@example.com", "grace@example.com")));
    }

    @Test
    void getOne_returnsSeededPerson() throws Exception {
        mockMvc.perform(get("/api/people/1").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1815-12-10"));
    }

    @Test
    void getOne_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/people/999").header("X-API-Key", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Person not found"))
                .andExpect(jsonPath("$.detail").value("Person with id 999 not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/people/999"))
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    void getPage_firstPageFromDatabase() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "2").param("numpage", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"))
                .andExpect(jsonPath("$.content[1].firstName").value("Alan"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void getPage_lastPageHoldsTheRemainder() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "2").param("numpage", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Grace"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void getPage_beyondLastPageIsEmptyWithTotals() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "2").param("numpage", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void getPage_rejectsInvalidSizeWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "0").param("numpage", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid page request"))
                .andExpect(jsonPath("$.detail").value("n must be >= 1 but was 0"))
                .andExpect(jsonPath("$.instance").value("/api/people/page"));
    }

    @Test
    void getPage_rejectsHugePageNumberInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "100").param("numpage", "30000000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid page request"))
                .andExpect(jsonPath("$.detail").value("numpage is too large for n=100"));
    }

    @Test
    void getPage_rejectsSizeAboveMax() throws Exception {
        mockMvc.perform(get("/api/people/page").header("X-API-Key", API_KEY).param("n", "101").param("numpage", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("n must be <= 100 but was 101"));
    }

    @Test
    void getAll_withoutKey_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/people"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", "ApiKey realm=\"people-api\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("API key missing"))
                .andExpect(jsonPath("$.detail").value("Missing X-API-Key header"))
                .andExpect(jsonPath("$.instance").value("/api/people"));
    }

    @Test
    void getAll_withBlankKey_returns401() throws Exception {
        mockMvc.perform(get("/api/people").header("X-API-Key", "  "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("API key missing"));
    }

    @Test
    void getAll_withWrongKey_returns403ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/people").header("X-API-Key", "wrong-key"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Invalid API key"))
                .andExpect(jsonPath("$.detail").value("The provided API key is not valid"))
                .andExpect(jsonPath("$.instance").value("/api/people"));
    }

    @Test
    void getPage_withoutKey_returns401() throws Exception {
        mockMvc.perform(get("/api/people/page").param("n", "2").param("numpage", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("API key missing"));
    }

    @Test
    void getOne_unknownIdWithoutKey_returns401NotFound() throws Exception {
        mockMvc.perform(get("/api/people/999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("API key missing"));
    }

    @Test
    void getOne_withWrongKey_returns403() throws Exception {
        mockMvc.perform(get("/api/people/1").header("X-API-Key", "wrong-key"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Invalid API key"));
    }

    @Test
    void disguisedApiPath_doesNotBypassTheKey() throws Exception {
        mockMvc.perform(get("/api;x=1/people"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("API key missing"));
    }
}
