package com.vantage.search.infrastructure.security;

import com.vantage.search.infrastructure.persistence.entity.UserEntity;
import com.vantage.search.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            userRepository.save(UserEntity.builder()
                    .username(adminUsername)
                    .password(adminPassword)
                    .role("ADMIN")
                    .build());
            log.info("Admin user '{}' bootstrapped.", adminUsername);
        }
    }
}
