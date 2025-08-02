package com.project.projectmanagementapplication.dto;

import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
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

    private User assignee;

    private Long projectId;

    private Project project;

    private List<Comment> comments = new ArrayList<>();
}
