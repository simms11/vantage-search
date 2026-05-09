package com.vantage.search.infrastructure.security;

import com.vantage.search.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VantageUserDetailsService implements UserDetailsService {

    public static final String USER_DETAILS_CACHE = "user-details";

    private final UserRepository userRepository;

    /**
     * Cached so the JWT filter does not hit Postgres on every authenticated
     * request. TTL is configured at the Redis cache manager (5 minutes), which
     * bounds how long a freshly-disabled user could keep authenticating.
     */
    @Override
    @Cacheable(value = USER_DETAILS_CACHE, unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .filter(u -> u.isEnabled())
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
