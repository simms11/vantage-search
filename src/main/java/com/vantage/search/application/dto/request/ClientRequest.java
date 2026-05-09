package com.vantage.search.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ClientRequest(
        @NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
        @NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 2000) String description,
        @JsonProperty("social_links") List<String> socialLinks
) {}
