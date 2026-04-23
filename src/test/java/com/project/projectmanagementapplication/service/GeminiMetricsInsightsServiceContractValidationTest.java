package com.project.projectmanagementapplication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.exception.InsightGenerationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiMetricsInsightsServiceContractValidationTest {

    private final GeminiMetricsInsightsService service =
            new GeminiMetricsInsightsService(null, new ObjectMapper(), "test-model");

    @Test
    void parseStructuredInsights_acceptsValidKanbanSections() {
        String json = sectionsJson(List.of(
                "overview",
                "what_these_metrics_mean",
                "key_observations",
                "main_insight",
                "trend_analysis",
                "team_throughput",
                "what_to_try_next",
                "coaching_note"));

        assertDoesNotThrow(() -> invokeParse(json, "KANBAN"));
    }

    @Test
    void parseStructuredInsights_acceptsValidScrumSections() {
        String json = sectionsJson(List.of(
                "overview",
                "what_these_metrics_mean",
                "key_observations",
                "main_insight",
                "trend_analysis",
                "team_velocity",
                "what_to_try_next",
                "coaching_note"));

        assertDoesNotThrow(() -> invokeParse(json, "SCRUM"));
    }

    @Test
    void parseStructuredInsights_rejectsWrongKeyOrder() {
        String json = sectionsJson(List.of(
                "overview",
                "what_these_metrics_mean",
                "main_insight",
                "key_observations",
                "trend_analysis",
                "team_throughput",
                "what_to_try_next",
                "coaching_note"));

        try {
            invokeParse(json, "KANBAN");
        } catch (InsightGenerationException e) {
            assertTrue(e.getMessage().contains("contract violation"));
            assertTrue(e.getMessage().contains("section 3 key must be 'key_observations'"));
            return;
        }
        throw new AssertionError("Expected InsightGenerationException");
    }

    @Test
    void parseStructuredInsights_rejectsWrongSectionCount() {
        String json = sectionsJson(List.of(
                "overview",
                "what_these_metrics_mean",
                "key_observations",
                "main_insight",
                "trend_analysis",
                "team_throughput",
                "what_to_try_next"));

        try {
            invokeParse(json, "KANBAN");
        } catch (InsightGenerationException e) {
            assertTrue(e.getMessage().contains("expected 8 sections"));
            return;
        }
        throw new AssertionError("Expected InsightGenerationException");
    }

    private Object invokeParse(String json, String framework) {
        try {
            Method m = GeminiMetricsInsightsService.class
                    .getDeclaredMethod("parseStructuredInsights", String.class, String.class);
            m.setAccessible(true);
            return m.invoke(service, json, framework);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof InsightGenerationException ie) {
                throw ie;
            }
            throw new RuntimeException(e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String sectionsJson(List<String> keys) {
        StringBuilder sb = new StringBuilder("{\"sections\":[");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            String key = keys.get(i);
            sb.append("{\"key\":\"")
                    .append(key)
                    .append("\",\"title\":\"")
                    .append(titleFromKey(key))
                    .append("\",\"content\":\"content ")
                    .append(i + 1)
                    .append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String titleFromKey(String key) {
        return switch (key) {
            case "overview" -> "Overview";
            case "what_these_metrics_mean" -> "What These Metrics Mean";
            case "key_observations" -> "Key Observations";
            case "main_insight" -> "Main Insight";
            case "trend_analysis" -> "Trend Analysis";
            case "team_throughput" -> "Team Throughput";
            case "team_velocity" -> "Team Velocity";
            case "what_to_try_next" -> "What To Try Next";
            case "coaching_note" -> "Coaching Note";
            default -> key;
        };
    }
}
