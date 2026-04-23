package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.ColumnLimitRow;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.ColumnLimitsSection;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import com.project.projectmanagementapplication.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnLimitsMetricsAssemblerTest {

    @Test
    void countIssuesByStandardStatuses_countsOnlyBoardStatuses() {
        Issue a = new Issue();
        a.setStatus(ISSUE_STATUS.TO_DO);
        Issue b = new Issue();
        b.setStatus(ISSUE_STATUS.TO_DO);
        Issue c = new Issue();
        c.setStatus(ISSUE_STATUS.IN_PROGRESS);

        Map<ISSUE_STATUS, Integer> m = ColumnLimitsMetricsAssembler.countIssuesByStandardStatuses(List.of(a, b, c));
        assertEquals(2, m.get(ISSUE_STATUS.TO_DO));
        assertEquals(1, m.get(ISSUE_STATUS.IN_PROGRESS));
        assertEquals(0, m.get(ISSUE_STATUS.DONE));
    }

    @Test
    void wipLimitsFromSettings_ignoresNullWip() {
        BoardColumnSetting row = new BoardColumnSetting();
        row.setStatus(ISSUE_STATUS.TO_DO);
        row.setWipLimit(null);

        Map<ISSUE_STATUS, Integer> m = ColumnLimitsMetricsAssembler.wipLimitsFromSettings(List.of(row));
        assertTrue(m.isEmpty());
    }

    @Test
    void buildSection_setsExceededAndOmitsMaxWhenUnconfigured() {
        Map<ISSUE_STATUS, Integer> counts = new EnumMap<>(ISSUE_STATUS.class);
        counts.put(ISSUE_STATUS.TO_DO, 4);
        counts.put(ISSUE_STATUS.IN_PROGRESS, 2);
        counts.put(ISSUE_STATUS.DONE, 1);

        Map<ISSUE_STATUS, Integer> limits = new EnumMap<>(ISSUE_STATUS.class);
        limits.put(ISSUE_STATUS.IN_PROGRESS, 2);

        ColumnLimitsSection sec = ColumnLimitsMetricsAssembler.buildSection(counts, limits);
        assertTrue(sec.isMaximumTaskLimitConfigured());

        ColumnLimitRow todo = sec.getColumns().stream()
                .filter(r -> "TO_DO".equals(r.getStatusKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(4, todo.getTaskCount());
        assertFalse(todo.isMaxTaskLimitConfigured());
        assertNull(todo.getMaxTasksAllowed());
        assertFalse(todo.isExceeded());

        ColumnLimitRow wip = sec.getColumns().stream()
                .filter(r -> "IN_PROGRESS".equals(r.getStatusKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, wip.getTaskCount());
        assertTrue(wip.isMaxTaskLimitConfigured());
        assertEquals(2, wip.getMaxTasksAllowed());
        assertFalse(wip.isExceeded());

        ColumnLimitRow done = sec.getColumns().stream()
                .filter(r -> "DONE".equals(r.getStatusKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, done.getTaskCount());
        assertFalse(done.isMaxTaskLimitConfigured());
    }

    @Test
    void buildSection_marksExceededWhenOverLimit() {
        Map<ISSUE_STATUS, Integer> counts = new EnumMap<>(ISSUE_STATUS.class);
        counts.put(ISSUE_STATUS.TO_DO, 5);
        Map<ISSUE_STATUS, Integer> limits = new EnumMap<>(ISSUE_STATUS.class);
        limits.put(ISSUE_STATUS.TO_DO, 3);

        ColumnLimitsSection sec = ColumnLimitsMetricsAssembler.buildSection(counts, limits);
        ColumnLimitRow todo = sec.getColumns().get(0);
        assertTrue(todo.isExceeded());
    }
}
