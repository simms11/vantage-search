package com.vantage.search_api.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 100_000) String content
) {}
