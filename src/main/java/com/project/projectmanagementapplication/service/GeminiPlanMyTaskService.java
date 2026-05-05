package com.project.projectmanagementapplication.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.project.projectmanagementapplication.dto.ai.MetricsInsightsResponse;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightSection;
import com.project.projectmanagementapplication.dto.ai.PlutoInsightsStructuredResponse;
import com.project.projectmanagementapplication.exception.InsightGenerationException;
import com.project.projectmanagementapplication.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnBean(Client.class)
public class GeminiPlanMyTaskService {

    private static final String PROMPT_RESOURCE = "prompts/pluto-plan-my-tasks-system.txt";
    private static final String SYSTEM_PROMPT = loadResource(PROMPT_RESOURCE);
    private static final List<String> EXPECTED_SECTION_KEYS = List.of(
            "summary", "priority_order", "timeline_check", "at_risk", "recommendation");

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public GeminiPlanMyTaskService(
            Client client,
            ObjectMapper objectMapper,
            @Value("${gemini.model}") String modelId) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.modelId = modelId;
    }

    public MetricsInsightsResponse generatePlan(List<Issue> tasks, String projectName, LocalDate today) {
        String userMessage;
        try {
            userMessage = buildUserMessage(tasks, projectName, today);
        } catch (JsonProcessingException e) {
            throw new InsightGenerationException("Failed to serialize task plan payload", e);
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_PROMPT)))
                    .build();

            GenerateContentResponse response = client.models.generateContent(modelId, userMessage, config);
            String text = response != null ? response.text() : null;
            if (text == null || text.isBlank()) {
                throw new InsightGenerationException("Model returned empty text for task plan");
            }

            PlutoInsightsStructuredResponse parsedInsights = parseStructuredInsights(text);

            return MetricsInsightsResponse.builder()
                    .model(modelId)
                    .generatedText(text.trim())
                    .parsedInsights(parsedInsights)
                    .build();
        } catch (InsightGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini generateContent failed for task plan: {}", e.getClass().getSimpleName());
            throw new InsightGenerationException("Unable to generate task plan", e);
        }
    }

    private String buildUserMessage(List<Issue> tasks, String projectName, LocalDate today)
            throws JsonProcessingException {
        List<TaskItem> items = tasks.stream()
                .map(issue -> {
                    Long daysUntilDue = null;
                    if (issue.getDueDate() != null) {
                        daysUntilDue = ChronoUnit.DAYS.between(today, issue.getDueDate());
                    }
                    return new TaskItem(
                            issue.getId(),
                            issue.getTitle(),
                            issue.getDescription(),
                            issue.getPriority() != null ? issue.getPriority().name() : null,
                            issue.getStatus() != null ? issue.getStatus().name() : null,
                            issue.getDueDate() != null ? issue.getDueDate().toString() : null,
                            daysUntilDue);
                })
                .toList();

        Payload payload = new Payload(today.toString(), projectName, items);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

        return """
                Generate the task plan exactly as instructed.

                Important:
                - Return valid JSON only.
                - Use exactly the 5 required sections with the exact keys and titles.
                - Do not output plain text before or after the JSON.
                - Use only the task data below.

                Today's date and task list:
                """ + json;
    }

    private PlutoInsightsStructuredResponse parseStructuredInsights(String text) {
        String json = normalizeModelJson(text);
        try {
            PlutoInsightsStructuredResponse parsed =
                    objectMapper.readValue(json, PlutoInsightsStructuredResponse.class);
            validateSectionsContract(parsed);
            return parsed;
        } catch (InsightGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new InsightGenerationException("Failed to parse Gemini JSON response for task plan", e);
        }
    }

    private void validateSectionsContract(PlutoInsightsStructuredResponse parsed) {
        List<PlutoInsightSection> sections = parsed.getSections();
        if (sections == null || sections.size() != EXPECTED_SECTION_KEYS.size()) {
            throw new InsightGenerationException(
                    "Task plan contract violation: expected "
                            + EXPECTED_SECTION_KEYS.size() + " sections, got "
                            + (sections == null ? 0 : sections.size()));
        }
        for (int i = 0; i < EXPECTED_SECTION_KEYS.size(); i++) {
            PlutoInsightSection section = sections.get(i);
            String expectedKey = EXPECTED_SECTION_KEYS.get(i);
            String actualKey = section != null ? section.getKey() : null;
            if (!Objects.equals(expectedKey, actualKey)) {
                throw new InsightGenerationException(
                        "Task plan contract violation: section " + (i + 1)
                                + " key must be '" + expectedKey + "' but was '" + actualKey + "'");
            }
            if (section.getTitle() == null || section.getTitle().isBlank()
                    || section.getContent() == null || section.getContent().isBlank()) {
                throw new InsightGenerationException(
                        "Task plan contract violation: section '" + expectedKey
                                + "' has blank title or content");
            }
        }
    }

    private static String loadResource(String classpathResource) {
        try (InputStream in =
                GeminiPlanMyTaskService.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + classpathResource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + classpathResource, e);
        }
    }

    static String normalizeModelJson(String text) {
        if (text == null) return "";
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
        if (start < 0) return t;
        return extractBalancedJsonObject(t, start);
    }

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
                if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return t.substring(start, i + 1);
            }
        }
        return t.substring(start);
    }

    record Payload(String today, String projectName, List<TaskItem> tasks) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TaskItem(
            Long id,
            String title,
            String description,
            String priority,
            String status,
            String dueDate,
            Long daysUntilDue) {}
}
