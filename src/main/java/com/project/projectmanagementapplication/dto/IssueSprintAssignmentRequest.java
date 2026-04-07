package com.project.projectmanagementapplication.dto;

import lombok.Data;

@Data
public class IssueSprintAssignmentRequest {

    /** Null or omitted means move issue to the product backlog (no sprint). */
    private Long sprintId;
}
