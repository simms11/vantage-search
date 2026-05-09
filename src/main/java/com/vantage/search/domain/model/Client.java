package com.vantage.search.domain.model;

import java.util.Set;
import java.util.UUID;

public record Client(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        Set<String> socialLinks
) {}
