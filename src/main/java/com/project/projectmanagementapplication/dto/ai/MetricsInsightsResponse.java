package com.project.projectmanagementapplication.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class MetricsInsightsResponse {

    private String model;
    private String generatedText;
    private PlutoInsightsStructuredResponse parsedInsights;
    private AiMetricsContextPayload metricsContextUsed;
}
