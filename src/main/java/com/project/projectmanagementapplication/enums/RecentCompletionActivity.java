package com.project.projectmanagementapplication.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How completions are spread across calendar days in the cycle/lead trend window.
 * Computed in {@link com.project.projectmanagementapplication.service.AiMetricsContextServiceImpl}.
 */
public enum RecentCompletionActivity {
    SPARSE,
    MODERATE,
    STEADY;

    @JsonValue
    public String toJson() {
        return name();
    }
}
