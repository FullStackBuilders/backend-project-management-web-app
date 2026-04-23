package com.project.projectmanagementapplication.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightSection;
import com.project.projectmanagementapplication.dto.ai.MetricsInsightsResponse;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightsStructuredResponse;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.exception.InsightGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
@ConditionalOnBean(Client.class)
public class GeminiMetricsInsightsService {

    private static final String KANBAN_PROMPT_RESOURCE = "prompts/pluto-kanban-metrics-insights-system.txt";
    private static final String SCRUM_PROMPT_RESOURCE = "prompts/pluto-scrum-metrics-insights-system.txt";

    private static final Map<String, String> SYSTEM_PROMPTS_BY_FRAMEWORK = Map.of(
            PROJECT_FRAMEWORK.KANBAN.name(), loadResource(KANBAN_PROMPT_RESOURCE),
            PROJECT_FRAMEWORK.SCRUM.name(), loadResource(SCRUM_PROMPT_RESOURCE));
    private static final Map<String, List<String>> EXPECTED_SECTION_KEYS_BY_FRAMEWORK = Map.of(
            PROJECT_FRAMEWORK.KANBAN.name(),
            List.of(
                    "overview",
                    "what_these_metrics_mean",
                    "key_observations",
                    "main_insight",
                    "trend_analysis",
                    "team_throughput",
                    "what_to_try_next",
                    "coaching_note"),
            PROJECT_FRAMEWORK.SCRUM.name(),
            List.of(
                    "overview",
                    "what_these_metrics_mean",
                    "key_observations",
                    "main_insight",
                    "trend_analysis",
                    "team_velocity",
                    "what_to_try_next",
                    "coaching_note"));

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public GeminiMetricsInsightsService(
            Client client,
            ObjectMapper objectMapper,
            @Value("${gemini.model}") String modelId) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.modelId = modelId;
    }

    /**
     * Calls Gemini with the framework-specific Pluto system prompt and the given metrics payload.
     */
    public MetricsInsightsResponse generateInsights(AiMetricsContextPayload metricsContextUsed) {
        String userMessage;
        try {
            userMessage = buildUserMessage(metricsContextUsed);
        } catch (JsonProcessingException e) {
            throw new InsightGenerationException("Failed to serialize metrics context", e);
        }

        String framework = resolveFramework(metricsContextUsed);
        String systemInstruction = resolveSystemInstruction(framework);

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .build();

            GenerateContentResponse response = client.models.generateContent(modelId, userMessage, config);
            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new InsightGenerationException("Model returned empty text");
            }
            PlutoInsightsStructuredResponse parsedInsights = parseStructuredInsights(text, framework);

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

    private String resolveFramework(AiMetricsContextPayload payload) {
        if (payload.getContext() == null || payload.getContext().getFramework() == null) {
            throw new InsightGenerationException("metrics context.framework is required for AI insights");
        }
        return payload.getContext().getFramework().trim().toUpperCase();
    }

    private String resolveSystemInstruction(String framework) {
        String fw = framework.trim().toUpperCase();
        String text = SYSTEM_PROMPTS_BY_FRAMEWORK.get(fw);
        if (text == null) {
            throw new InsightGenerationException("Unsupported framework for AI insights: " + framework);
        }
        return text;
    }

    private String buildUserMessage(AiMetricsContextPayload payload) throws JsonProcessingException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        String framework = resolveFramework(payload);
        return """
        Generate the metrics insight response exactly as instructed.

        Important:
        - Return valid JSON only.
        - Use exactly the required 8 sections.
        - Use exactly the required keys and titles for framework: %s.
        - Do not output plain text before or after the JSON.
        - Do not invent numbers, dates, or tasks.
        - Use only the derived metrics below.

        Derived metrics for analysis:
        """.formatted(framework) + json;
    }

    private static String extractText(GenerateContentResponse response) {
        if (response == null) {
            return null;
        }
        return response.text();
    }

    private static String loadResource(String classpathResource) {
        try (InputStream in =
                GeminiMetricsInsightsService.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + classpathResource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + classpathResource, e);
        }
    }

    private PlutoInsightsStructuredResponse parseStructuredInsights(String text, String framework) {
        String json = normalizeModelJson(text);
        try {
            PlutoInsightsStructuredResponse parsed = objectMapper.readValue(json, PlutoInsightsStructuredResponse.class);
            validateSectionsContract(parsed, framework);
            return parsed;
        } catch (InsightGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new InsightGenerationException("Failed to parse Gemini JSON response", e);
        }
    }

    private void validateSectionsContract(PlutoInsightsStructuredResponse parsed, String framework) {
        List<String> expectedKeys = EXPECTED_SECTION_KEYS_BY_FRAMEWORK.get(framework);
        if (expectedKeys == null) {
            throw new InsightGenerationException("Unsupported framework for section validation: " + framework);
        }
        List<PlutoInsightSection> sections = parsed.getSections();
        if (sections == null || sections.size() != expectedKeys.size()) {
            throw new InsightGenerationException(
                    "Insights contract violation for framework " + framework + ": expected "
                            + expectedKeys.size() + " sections");
        }
        for (int i = 0; i < expectedKeys.size(); i++) {
            PlutoInsightSection section = sections.get(i);
            String expectedKey = expectedKeys.get(i);
            String actualKey = section != null ? section.getKey() : null;
            if (!Objects.equals(expectedKey, actualKey)) {
                throw new InsightGenerationException(
                        "Insights contract violation for framework " + framework + ": section " + (i + 1)
                                + " key must be '" + expectedKey + "' but was '" + actualKey + "'");
            }
            String title = section.getTitle();
            String content = section.getContent();
            if (title == null || title.isBlank() || content == null || content.isBlank()) {
                throw new InsightGenerationException(
                        "Insights contract violation for framework " + framework + ": section '" + expectedKey
                                + "' must include non-blank title and content");
            }
        }
    }

    /**
     * Strips optional markdown fences and isolates the outermost JSON object so Jackson can parse model output
     * that starts with {@code ```json} or has preamble before {@code {.
     */
    static String normalizeModelJson(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int afterOpen = t.indexOf('\n');
            if (afterOpen >= 0) {
                t = t.substring(afterOpen + 1);
            } else {
                t = t.substring(3).replaceFirst("(?i)^json\\s*", "");
            }
            int fenceEnd = t.lastIndexOf("```");
            if (fenceEnd >= 0) {
                t = t.substring(0, fenceEnd).trim();
            }
        }
        int start = t.indexOf('{');
        if (start < 0) {
            return t;
        }
        return extractBalancedJsonObject(t, start);
    }

    /**
     * From index of first '{', returns the substring through the matching '}', respecting strings and backslash escapes.
     */
    static String extractBalancedJsonObject(String t, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < t.length(); i++) {
            char c = t.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return t.substring(start, i + 1);
                }
            }
        }
        return t.substring(start);
    }
}
