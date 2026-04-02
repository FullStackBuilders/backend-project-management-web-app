package com.project.projectmanagementapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCountsResponse {

    private Long assignedTasks;

    private Long overdueTasks;

    private long dueTodayTasks;

    private Long highPriorityTasks;

    private Long completedTasks;


}
