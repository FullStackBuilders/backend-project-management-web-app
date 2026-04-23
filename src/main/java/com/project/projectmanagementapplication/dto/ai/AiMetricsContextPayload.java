package com.project.projectmanagementapplication.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.projectmanagementapplication.enums.RecentCompletionActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AiMetricsContextPayload {

    private ContextSection context;
    private PrimarySection primary;
    private TrendsSection trends;
    private DerivedSection derived;
    private HintsSection hints;
    /**
     * Board column task limits vs counts for AI grounding (Kanban: project-wide; Scrum: selected sprint only).
     * Prompt instructions may reference this in a later iteration.
     */
    private ColumnLimitsSection columnLimits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class ContextSection {
        private String framework;
        private long projectId;
        private String projectName;
        /** Kanban: human-readable time range label (e.g. Last 7 days). Scrum: omitted. */
        private String scopeLabel;
        /** Kanban: analysis window. Scrum: omitted. */
        private LocalDate scopeStartDate;
        /** Kanban: analysis window. Scrum: omitted. */
        private LocalDate scopeEndDate;
        private Instant generatedAt;

        /** Scrum: selected sprint id. */
        private Long sprintId;
        /** Scrum: display name. */
        private String sprintName;
        /** Scrum: optional sprint goal. */
        private String sprintGoal;
        /** Scrum: ACTIVE, COMPLETED, etc. */
        private String sprintStatus;
        /** Scrum: official sprint start from entity. */
        private LocalDate sprintStartDate;
        /** Scrum: official sprint end from entity. */
        private LocalDate sprintEndDate;
        /** Scrum: cycle/lead trend chart window start. */
        private LocalDate trendWindowStartDate;
        /** Scrum: cycle/lead trend chart window end. */
        private LocalDate trendWindowEndDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class PrimarySection {
        private int wip;
        private Double avgCycleTimeDays;
        private Double avgLeadTimeDays;
        /** Kanban only: completions in selected time range. Scrum: omit (null). */
        private Integer throughput;
        /** Scrum only: DONE task count in selected sprint. Kanban: omit (null). */
        private Integer velocity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class TrendsSection {
        private List<CycleLeadByDayPoint> cycleLeadByDay;
        private List<ThroughputByDayPoint> throughputByDay;
        private List<VelocityBySprintRow> velocityBySprint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class DerivedSection {
        private Double avgWaitingTimeDays;
        private Boolean leadVsCycleGapIsLarge;
        private Integer selectedSprintCompletedTasks;
        private Integer selectedSprintTotalTasks;
        /** 0.0–1.0 when sprint metrics present */
        private Double selectedSprintCompletionRate;
        /** Days in the cycle/lead window with {@code completedTasks > 0}. */
        private Integer completedDaysCount;
        /** Days in the window with zero completions. */
        private Integer zeroCompletionDaysCount;
        /** Spread of completion activity across the window; see {@link RecentCompletionActivity}. */
        private RecentCompletionActivity recentCompletionActivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintsSection {
        /**
         * True when {@link DerivedSection#getCompletedDaysCount()} is at least four: enough distinct days with a
         * completion in the trend window to describe a trend with reasonable confidence.
         */
        private boolean enoughTrendData;
        private List<String> notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class ColumnLimitsSection {
        /** True if at least one column has a non-null max task limit configured. */
        private boolean maximumTaskLimitConfigured;
        private List<ColumnLimitRow> columns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class ColumnLimitRow {
        private String statusKey;
        private String displayName;
        private int taskCount;
        /** Present only when {@link #maxTaskLimitConfigured} is true. */
        private Integer maxTasksAllowed;
        private boolean maxTaskLimitConfigured;
        /** Serialized as {@code isExceeded} for the model payload. */
        @JsonProperty("isExceeded")
        private boolean exceeded;
    }
}
