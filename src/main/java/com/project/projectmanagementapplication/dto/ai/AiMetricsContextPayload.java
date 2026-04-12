package com.project.projectmanagementapplication.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class ContextSection {
        private String framework;
        private long projectId;
        private String projectName;
        private String scopeLabel;
        private LocalDate scopeStartDate;
        private LocalDate scopeEndDate;
        private Instant generatedAt;
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
        private int throughput;
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
}
