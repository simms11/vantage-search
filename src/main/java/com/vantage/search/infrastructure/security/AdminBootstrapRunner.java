package com.vantage.search.infrastructure.security;

import com.vantage.search.infrastructure.persistence.entity.UserEntity;
import com.vantage.search.infrastructure.persistence.entity.UserRole;
import com.vantage.search.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Pattern ENCODED_PASSWORD_PREFIX = Pattern.compile("^\\{[a-zA-Z0-9]+}.+");

    private final UserRepository userRepository;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!ENCODED_PASSWORD_PREFIX.matcher(adminPassword).matches()) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD must be a Spring Security DelegatingPasswordEncoder string "
                            + "(e.g. {bcrypt}$2a$10$...). Generate with: "
                            + "htpasswd -bnBC 10 \"\" your_password | tr -d ':\\n' and prefix with {bcrypt}.");
        }

        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        try {
            userRepository.save(UserEntity.builder()
                    .username(adminUsername)
                    .password(adminPassword)
                    .role(UserRole.ADMIN)
                    .build());
            log.info("Admin user '{}' bootstrapped.", adminUsername);
        } catch (DataIntegrityViolationException ex) {
            log.info("Admin user '{}' already created by another instance; continuing.", adminUsername);
        }
    }
}
