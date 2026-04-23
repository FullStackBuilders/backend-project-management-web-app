package com.project.projectmanagementapplication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightsStructuredResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiMetricsInsightsServiceNormalizeJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void stripsMarkdownFenceAndParses() throws Exception {
        String raw = "```json\n{ \"sections\": [] }\n```";
        String normalized = GeminiMetricsInsightsService.normalizeModelJson(raw);
        PlutoInsightsStructuredResponse parsed =
                objectMapper.readValue(normalized, PlutoInsightsStructuredResponse.class);
        assertNotNull(parsed.getSections());
        assertTrue(parsed.getSections().isEmpty());
    }

    @Test
    void plainJsonUnchangedShape() throws Exception {
        String raw = "{ \"sections\": [] }";
        String normalized = GeminiMetricsInsightsService.normalizeModelJson(raw);
        assertEquals(raw.replace(" ", ""), normalized.replace(" ", ""));
    }

    @Test
    void preambleBeforeObject() throws Exception {
        String raw = "Here is the JSON:\n{\"sections\":[]}";
        String normalized = GeminiMetricsInsightsService.normalizeModelJson(raw);
        PlutoInsightsStructuredResponse parsed =
                objectMapper.readValue(normalized, PlutoInsightsStructuredResponse.class);
        assertNotNull(parsed.getSections());
        assertTrue(parsed.getSections().isEmpty());
    }

    @Test
    void balancedExtractIgnoresBraceInsideString() throws Exception {
        String inner =
                "{\"sections\":[{\"key\":\"k\",\"title\":\"t\",\"content\":\"literal } brace\"}]}";
        String raw = "```\n" + inner + "\n```";
        String normalized = GeminiMetricsInsightsService.normalizeModelJson(raw);
        PlutoInsightsStructuredResponse parsed =
                objectMapper.readValue(normalized, PlutoInsightsStructuredResponse.class);
        assertEquals(1, parsed.getSections().size());
        assertEquals("literal } brace", parsed.getSections().get(0).getContent());
    }
}
