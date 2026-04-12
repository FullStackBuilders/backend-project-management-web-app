package com.project.projectmanagementapplication.dto.ai;

import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityBySprintRow {
    private Long sprintId;
    private String sprintName;
    private int completedTasks;
    private SPRINT_STATUS sprintStatus;
}
