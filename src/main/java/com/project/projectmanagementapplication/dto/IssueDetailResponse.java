package com.project.projectmanagementapplication.dto;
import com.project.projectmanagementapplication.model.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private Long createdById;
    private String createdByName;

    private Long assigneeId;
    private String assigneeName;

    private Long projectId;
    private String projectName;
    private Long projectOwnerId;
    private String projectOwnerName;

    private List<CommentResponse> comments;

    private String lastEditedByName;
    private LocalDateTime lastEditedAt;
}