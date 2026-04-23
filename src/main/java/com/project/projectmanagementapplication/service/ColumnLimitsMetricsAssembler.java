package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.ColumnLimitRow;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.ColumnLimitsSection;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import com.project.projectmanagementapplication.model.Issue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ColumnLimitsSection} for AI metrics context: Kanban uses project-wide issues; Scrum uses sprint-scoped issues.
 */
public final class ColumnLimitsMetricsAssembler {

    private static final List<ISSUE_STATUS> BOARD_STATUSES =
            List.of(ISSUE_STATUS.TO_DO, ISSUE_STATUS.IN_PROGRESS, ISSUE_STATUS.DONE);

    private ColumnLimitsMetricsAssembler() {}

    public static Map<ISSUE_STATUS, Integer> countIssuesByStandardStatuses(List<Issue> issues) {
        Map<ISSUE_STATUS, Integer> m = new EnumMap<>(ISSUE_STATUS.class);
        for (ISSUE_STATUS s : BOARD_STATUSES) {
            m.put(s, 0);
        }
        for (Issue i : issues) {
            ISSUE_STATUS st = i.getStatus();
            if (st != null && m.containsKey(st)) {
                m.merge(st, 1, Integer::sum);
            }
        }
        return m;
    }

    /**
     * Only statuses with a non-null {@link BoardColumnSetting#getWipLimit()} are present in the map.
     */
    public static Map<ISSUE_STATUS, Integer> wipLimitsFromSettings(List<BoardColumnSetting> rows) {
        Map<ISSUE_STATUS, Integer> m = new EnumMap<>(ISSUE_STATUS.class);
        for (BoardColumnSetting row : rows) {
            Integer lim = row.getWipLimit();
            if (lim != null) {
                m.put(row.getStatus(), lim);
            }
        }
        return m;
    }

    public static ColumnLimitsSection buildSection(
            Map<ISSUE_STATUS, Integer> taskCountByStatus, Map<ISSUE_STATUS, Integer> wipLimitByStatus) {
        boolean maximumTaskLimitConfigured =
                BOARD_STATUSES.stream().anyMatch(st -> wipLimitByStatus.get(st) != null);

        List<ColumnLimitRow> columns = new ArrayList<>(BOARD_STATUSES.size());
        for (ISSUE_STATUS st : BOARD_STATUSES) {
            int taskCount = taskCountByStatus.getOrDefault(st, 0);
            Integer max = wipLimitByStatus.get(st);
            boolean configured = max != null;
            boolean exceeded = configured && taskCount > max;
            columns.add(ColumnLimitRow.builder()
                    .statusKey(st.name())
                    .displayName(displayName(st))
                    .taskCount(taskCount)
                    .maxTasksAllowed(configured ? max : null)
                    .maxTaskLimitConfigured(configured)
                    .exceeded(exceeded)
                    .build());
        }
        return ColumnLimitsSection.builder()
                .maximumTaskLimitConfigured(maximumTaskLimitConfigured)
                .columns(columns)
                .build();
    }

    private static String displayName(ISSUE_STATUS s) {
        return switch (s) {
            case TO_DO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Done";
        };
    }
}
