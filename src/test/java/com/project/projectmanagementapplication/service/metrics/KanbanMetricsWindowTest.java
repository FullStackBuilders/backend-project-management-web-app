package com.project.projectmanagementapplication.service.metrics;

import com.project.projectmanagementapplication.enums.MetricsTimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KanbanMetricsWindowTest {

    @Test
    void last7Days_startsSixDaysBeforeToday() {
        LocalDate today = LocalDate.of(2026, 4, 10);
        assertEquals(LocalDate.of(2026, 4, 4), KanbanMetricsWindow.rangeStart(MetricsTimeRange.LAST_7_DAYS, today));
    }

    @Test
    void thisMonth_startsOnFirstOfMonth() {
        LocalDate today = LocalDate.of(2026, 4, 15);
        assertEquals(LocalDate.of(2026, 4, 1), KanbanMetricsWindow.rangeStart(MetricsTimeRange.THIS_MONTH, today));
    }

    @Test
    void thisWeek_startsOnMondaySameWeek() {
        LocalDate thursday = LocalDate.of(2026, 4, 9); // Thursday
        assertEquals(LocalDate.of(2026, 4, 6), KanbanMetricsWindow.rangeStart(MetricsTimeRange.THIS_WEEK, thursday));
    }
}
