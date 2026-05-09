package com.vantage.search.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.search.support.BaseIT;
import com.vantage.search.application.dto.request.ClientRequest;
import com.vantage.search.application.dto.request.DocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@AutoConfigureMockMvc
public class ClientControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE documents, client_social_links, clients CASCADE");
    }

    @Test
    @DisplayName("POST /api/clients - Success with snake_case")
    void shouldCreateClientSuccessfully() throws Exception {
        // GIVEN
        String json = """
            {
                "first_name": "John",
                "last_name": "Doe",
                "email": "john.doe@vantage.com",
                "description": "Premium Client",
                "social_links": ["https://linkedin.com/in/johndoe"]
            }
            """;

        // WHEN & THEN
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@vantage.com"));
    }

    @Test
    @DisplayName("POST /api/clients - Fail on invalid email")
    void shouldReturn400ForInvalidEmail() throws Exception {
        // given
        ClientRequest invalidRequest = new ClientRequest(
                "John", "Doe", "not-an-email", "Desc", Set.of()
        );

        // WHEN & THEN
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/clients/{id}/documents - Success")
    void shouldCreateDocumentForClient() throws Exception {
        String clientJson = """
            {
                "first_name": "Doc",
                "last_name": "Owner",
                "email": "doc.owner@vantage.com",
                "social_links": []
            }
            """;

        String responseString = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String clientId = objectMapper.readTree(responseString).get("id").asText();

        DocumentRequest docRequest = new DocumentRequest(
                "Tax Record",
                "Content of the document to be embedded and summarized."
        );

        // 3. WHEN & THEN
        mockMvc.perform(post("/api/clients/" + clientId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tax Record"))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.explanation").exists());
    }
}