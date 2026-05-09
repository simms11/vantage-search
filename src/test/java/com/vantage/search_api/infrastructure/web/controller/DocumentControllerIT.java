package com.vantage.search_api.infrastructure.web.controller;

import com.vantage.search_api.application.port.in.SummariseUseCase;
import com.vantage.search_api.support.BaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureMockMvc
public class DocumentControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummariseUseCase summariseUseCase;

    @Test
    @DisplayName("Should successfully return a bulleted summary from raw text")
    void shouldReturnDocumentSummary() throws Exception {
        String expectedSummary = "- Point 1\n- Point 2\n- Point 3";
        when(summariseUseCase.summarise(anyString())).thenReturn(expectedSummary);

        mockMvc.perform(post("/api/documents/summary")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("This is a long financial document that needs summarizing..."))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedSummary));
    }
}
