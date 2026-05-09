package com.vantage.search.infrastructure.security;

import com.vantage.search.config.AdminProperties;
import com.vantage.search.infrastructure.persistence.entity.UserEntity;
import com.vantage.search.infrastructure.persistence.entity.UserRole;
import com.vantage.search.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminProperties.username())) {
            return;
        }

        try {
            userRepository.save(UserEntity.builder()
                    .username(adminProperties.username())
                    .password(adminProperties.password())
                    .role(UserRole.ADMIN)
                    .build());
            log.info("Admin user '{}' bootstrapped.", adminProperties.username());
        } catch (DataIntegrityViolationException ex) {
            log.info("Admin user '{}' already created by another instance; continuing.",
                    adminProperties.username());
        }
    }
}
