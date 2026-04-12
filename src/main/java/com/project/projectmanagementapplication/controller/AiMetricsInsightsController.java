package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.MetricsContextRequest;
import com.project.projectmanagementapplication.dto.ai.MetricsInsightsResponse;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.AiMetricsContextService;
import com.project.projectmanagementapplication.service.GeminiMetricsInsightsService;
import com.project.projectmanagementapplication.service.UserService;
import com.google.genai.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(Client.class)
public class AiMetricsInsightsController {

    private final AiMetricsContextService aiMetricsContextService;
    private final GeminiMetricsInsightsService geminiMetricsInsightsService;
    private final UserService userService;

    public AiMetricsInsightsController(
            AiMetricsContextService aiMetricsContextService,
            GeminiMetricsInsightsService geminiMetricsInsightsService,
            UserService userService) {
        this.aiMetricsContextService = aiMetricsContextService;
        this.geminiMetricsInsightsService = geminiMetricsInsightsService;
        this.userService = userService;
    }


    @PostMapping("/metrics-insights")
    public ResponseEntity<Response<MetricsInsightsResponse>> metricsInsights(
            @RequestBody MetricsContextRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        AiMetricsContextPayload payload = aiMetricsContextService.buildContext(request, user);
        MetricsInsightsResponse insights = geminiMetricsInsightsService.generateInsights(payload);
        return ResponseEntity.ok(
                Response.<MetricsInsightsResponse>builder()
                        .message("Metrics insights generated successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now().toString())
                        .data(insights)
                        .build());
    }
}
