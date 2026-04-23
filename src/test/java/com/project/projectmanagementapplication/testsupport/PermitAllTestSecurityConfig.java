package com.project.projectmanagementapplication.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security for slice tests: no JWT filter, all requests permitted so MockMvc can
 * drive {@link org.springframework.security.core.Authentication} via
 * {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors}.
 */
@TestConfiguration
@EnableWebSecurity
public class PermitAllTestSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain permitAllTestChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
