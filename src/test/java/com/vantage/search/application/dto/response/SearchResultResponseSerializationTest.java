package com.vantage.search.application.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.search.domain.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class SearchResultResponseSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void clientMatchSerialisesWithTypeDiscriminator() throws Exception {
        SearchResultResponse response = SearchResultResponse.ClientMatch.from(new SearchResult.ClientMatch(
                UUID.randomUUID(), "John", "Doe", "john@vantage.com",
                "Desc", Set.of("link"), 1.0, "Explanation"));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"firstName\":\"John\"");
        assertThat(json).contains("\"lastName\":\"Doe\"");
        assertThat(json).contains("\"type\":\"client\"");
    }
}
