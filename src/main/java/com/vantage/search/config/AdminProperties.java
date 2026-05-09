package com.vantage.search.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated at bean binding so a missing or unprefixed admin password fails
 * before the embedded servlet starts accepting traffic. The previous
 * {@link com.vantage.search.infrastructure.security.AdminBootstrapRunner} check
 * fired only after the application was already serving requests.
 */
@Validated
@ConfigurationProperties("app.admin")
public record AdminProperties(
        @NotBlank String username,

        @NotBlank
        @Pattern(
                regexp = "^\\{[a-zA-Z0-9]+}.+",
                message = "app.admin.password must be a Spring Security DelegatingPasswordEncoder string "
                        + "(e.g. {bcrypt}$2a$10$...). Generate: "
                        + "htpasswd -bnBC 10 \"\" your_password | tr -d ':\\n' and prefix with {bcrypt}.")
        String password
) {}
