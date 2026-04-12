package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.MetricsContextRequest;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.AiMetricsContextService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ai")
public class AiMetricsContextController {

    private final AiMetricsContextService aiMetricsContextService;
    private final UserService userService;

    public AiMetricsContextController(
            AiMetricsContextService aiMetricsContextService,
            UserService userService) {
        this.aiMetricsContextService = aiMetricsContextService;
        this.userService = userService;
    }

    /**
     * Metrics payload for Pluto
     */
    @PostMapping("/metrics-context")
    public ResponseEntity<Response<AiMetricsContextPayload>> metricsContext(
            @RequestBody MetricsContextRequest request,
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        AiMetricsContextPayload data = aiMetricsContextService.buildContext(request, user);
        return ResponseEntity.ok(
                Response.<AiMetricsContextPayload>builder()
                        .message("Metrics context built successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now().toString())
                        .data(data)
                        .build());
    }
}
