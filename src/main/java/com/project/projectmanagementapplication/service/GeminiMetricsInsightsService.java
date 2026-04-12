package com.project.projectmanagementapplication.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.MetricsInsightsResponse;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightsStructuredResponse;
import com.project.projectmanagementapplication.exception.InsightGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Gemini text generation using the same Java pattern as the official docs:
 * <a href="https://ai.google.dev/gemini-api/docs/text-generation">Text generation</a> (system instructions) and
 * <a href="https://ai.google.dev/gemini-api/docs/quickstart#java">Quickstart (Java)</a> — {@code Client.builder().apiKey(...)},
 * {@code GenerateContentConfig.builder()...systemInstruction(Content.fromParts(Part.fromText(...)))},
 * {@code client.models.generateContent(model, contents, config)}, {@code response.text()}.
 */
@Slf4j
@Service
@ConditionalOnBean(Client.class)
public class GeminiMetricsInsightsService {

    private static final String SYSTEM_PROMPT_RESOURCE = "prompts/pluto-metrics-insights-system.txt";

    private static final String SYSTEM_INSTRUCTION = loadSystemPrompt();

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public GeminiMetricsInsightsService(
            Client client,
            ObjectMapper objectMapper,
            @Value("${gemini.model:gemini-2.5-flash}") String modelId) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.modelId = modelId;
    }

    /**
     * Calls Gemini with the fixed Pluto system prompt and the given metrics payload (serialized as JSON in the user turn).
     */
    public MetricsInsightsResponse generateInsights(AiMetricsContextPayload metricsContextUsed) {
        String userMessage;
        try {
            userMessage = buildUserMessage(metricsContextUsed);
        } catch (JsonProcessingException e) {
            throw new InsightGenerationException("Failed to serialize metrics context", e);
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                    .build();

            GenerateContentResponse response = client.models.generateContent(modelId, userMessage, config);
            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new InsightGenerationException("Model returned empty text");
            }
            PlutoInsightsStructuredResponse parsedInsights = parseStructuredInsights(text);

            return MetricsInsightsResponse.builder()
                    .model(modelId)
                    .generatedText(text.trim())
                    .parsedInsights(parsedInsights)
                    .metricsContextUsed(metricsContextUsed)
                    .build();
        } catch (InsightGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini generateContent failed: {}", e.getClass().getSimpleName());
            throw new InsightGenerationException("Unable to generate insights", e);
        }
    }

    private String buildUserMessage(AiMetricsContextPayload payload) throws JsonProcessingException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        return """
        Generate the metrics insight response exactly as instructed.

        Important:
        - Return valid JSON only.
        - Use exactly the required 8 sections.
        - Use exactly the required keys and titles.
        - Do not output plain text before or after the JSON.
        - Do not invent numbers, dates, or tasks.
        - Use only the derived metrics below.

        Derived metrics for analysis:
        """ + json;
    }

    private static String extractText(GenerateContentResponse response) {
        if (response == null) {
            return null;
        }
        return response.text();
    }

    private static String loadSystemPrompt() {
        try (InputStream in =
                GeminiMetricsInsightsService.class.getClassLoader().getResourceAsStream(SYSTEM_PROMPT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + SYSTEM_PROMPT_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + SYSTEM_PROMPT_RESOURCE, e);
        }
    }
    private PlutoInsightsStructuredResponse parseStructuredInsights(String text) {
        try {
            return objectMapper.readValue(text, PlutoInsightsStructuredResponse.class);
        } catch (Exception e) {
            throw new InsightGenerationException("Failed to parse Gemini JSON response", e);
        }
    }
}
