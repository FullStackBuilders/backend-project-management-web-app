package com.project.projectmanagementapplication.service.metrics;

import com.project.projectmanagementapplication.enums.MetricsTimeRange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Date range start for Kanban metrics; mirrors {@code getRangeBounds} in KanbanMetrics.jsx.
 */
public final class KanbanMetricsWindow {

    private KanbanMetricsWindow() {}

    public static LocalDate rangeStart(MetricsTimeRange timeRange, LocalDate today) {
        return switch (timeRange) {
            case THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case LAST_7_DAYS -> today.minusDays(6);
            case THIS_MONTH -> today.withDayOfMonth(1);
        };
    }

    public static String scopeLabel(MetricsTimeRange timeRange) {
        return switch (timeRange) {
            case THIS_WEEK -> "This Week";
            case LAST_7_DAYS -> "Last 7 Days";
            case THIS_MONTH -> "This Month";
        };
    }
}
