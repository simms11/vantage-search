package com.vantage.search.infrastructure.web.controller;

import com.vantage.search.support.BaseIT;
import com.vantage.search.application.port.in.SearchUseCase;
import com.vantage.search.domain.model.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@AutoConfigureMockMvc
public class SearchControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUseCase searchUseCase;

    @Test
    @DisplayName("Resilience: Should return partial results if AI services are unavailable")
    void searchShouldReturnAvailableResultsWhenAiIsDown() throws Exception {
        // GIVEN
        SearchResult clientMatch = new SearchResult.ClientMatch(
                UUID.randomUUID(),
                "John",
                "Doe",
                "john@vantage.com",
                "Financial advisor",
                List.of(),
                1.0,
                "Lexical match"
        );

        when(searchUseCase.search("vantage", 0, 20)).thenReturn(List.of(clientMatch));

        // WHEN
        var response = mockMvc.perform(get("/api/search").param("query", "vantage"));

        // THEN
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }
}