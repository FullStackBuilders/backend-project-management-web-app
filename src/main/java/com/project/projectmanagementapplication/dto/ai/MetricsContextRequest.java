package com.project.projectmanagementapplication.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.projectmanagementapplication.enums.MetricsTimeRange;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
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
public class MetricsContextRequest {

    private Long projectId;
    private PROJECT_FRAMEWORK framework;
    /** Required when framework is KANBAN. */
    private MetricsTimeRange timeRange;
    /** Required when framework is SCRUM. */
    private Long sprintId;
}
