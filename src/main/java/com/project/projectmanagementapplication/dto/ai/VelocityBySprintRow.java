package com.project.projectmanagementapplication.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class VelocityBySprintRow {
    private Long sprintId;
    private String sprintName;
    private int completedTasks;
    private SPRINT_STATUS sprintStatus;
    /** 0.0–1.0 when the sprint has at least one task; null if zero tasks in sprint. */
    private Double completionRate;
    /** True when this row is the sprint currently selected for analysis. */
    private boolean selectedSprint;
}
