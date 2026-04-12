package com.project.projectmanagementapplication.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One day in the cycle/lead trend. {@code avg*} may be null; {@code completedTasks} is always set (0 if none).
 * No {@code JsonInclude.NON_NULL} so JSON includes explicit nulls for Gemini.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CycleLeadByDayPoint {
    private LocalDate date;
    private Double avgCycleTimeDays;
    private Double avgLeadTimeDays;
    /** Issues with taskCompletedAt on this date (same basis as daily averages). */
    private int completedTasks;
}
