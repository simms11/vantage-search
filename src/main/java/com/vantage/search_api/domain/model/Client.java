package com.vantage.search_api.domain.model;

import java.util.List;
import java.util.UUID;

public record Client(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        List<String> socialLinks
) {}
