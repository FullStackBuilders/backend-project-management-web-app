package com.project.projectmanagementapplication.dto;
import com.project.projectmanagementapplication.model.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class IssueDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate assignedDate;
    private LocalDate dueDate;

    // Issue creator details
    private Long createdById;
    private String createdByName;

    // Assignee details
    private Long assigneeId;
    private String assigneeName;

    // Project details
    private Long projectId;
    private String projectName;
    private Long projectOwnerId;
    private String projectOwnerName;

    // Comments
    private List<Comment> comments;
}
