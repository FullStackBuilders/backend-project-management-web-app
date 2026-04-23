package com.project.projectmanagementapplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.MetricsContextRequest;
import com.project.projectmanagementapplication.enums.MetricsTimeRange;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.filter.JwtFilter;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.AiMetricsContextService;
import com.project.projectmanagementapplication.service.UserService;
import com.project.projectmanagementapplication.testsupport.PermitAllTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AiMetricsContextController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@Import(PermitAllTestSecurityConfig.class)
class AiMetricsContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiMetricsContextService aiMetricsContextService;

    @MockitoBean
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("pluto@example.com");
        when(userService.findByUsername("pluto@example.com")).thenReturn(user);
    }

    @Test
    void metricsContext_returnsWrappedPayload() throws Exception {
        AiMetricsContextPayload payload =
                AiMetricsContextPayload.builder()
                        .context(
                                AiMetricsContextPayload.ContextSection.builder()
                                        .framework("KANBAN")
                                        .projectId(42L)
                                        .projectName("Demo")
                                        .generatedAt(Instant.parse("2026-04-21T12:00:00Z"))
                                        .build())
                        .build();
        when(aiMetricsContextService.buildContext(any(MetricsContextRequest.class), eq(user)))
                .thenReturn(payload);

        MetricsContextRequest request =
                MetricsContextRequest.builder()
                        .projectId(42L)
                        .framework(PROJECT_FRAMEWORK.KANBAN)
                        .timeRange(MetricsTimeRange.LAST_7_DAYS)
                        .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "pluto@example.com",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(
                        post("/api/ai/metrics-context")
                                .with(authentication(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Metrics context built successfully"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.context.projectId").value(42))
                .andExpect(jsonPath("$.data.context.projectName").value("Demo"));

        verify(aiMetricsContextService).buildContext(any(MetricsContextRequest.class), eq(user));
    }
}
