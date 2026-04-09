package com.project.projectmanagementapplication.dto;

import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class IssueResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate assignedDate;
    private LocalDate dueDate;

    private Long createdById;
    private String createdByName;

    private Long assigneeId;
    private String assigneeName;

    private Long assignedById;
    private String assignedByName;

    private Long projectId;
    private Long projectOwnerId;

    /** Null when the issue is in the product backlog (no sprint). */
    private Long sprintId;
    private String sprintName;
    /**
     * Sprint lifecycle status when {@link #sprintId} is non-null (e.g. ACTIVE, COMPLETED).
     * Null for backlog issues.
     */
    private String sprintStatus;

    private Integer commentCount;

    private String lastUpdatedByName;
    private LocalDateTime lastUpdatedAt;

    // Workflow timestamps for Kanban metrics
    private LocalDateTime taskCreatedAt;    // sourced from AuditableEntity.createdAt — for Lead Time
    private LocalDateTime taskStartedAt;    // first IN_PROGRESS entry — for Cycle Time
    private LocalDateTime taskCompletedAt;  // DONE entry — for Cycle Time, Lead Time, Throughput
}
