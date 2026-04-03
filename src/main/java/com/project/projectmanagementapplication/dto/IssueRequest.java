package com.project.projectmanagementapplication.dto;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import lombok.Data;

import java.time.LocalDate;


@Data
public class IssueRequest {

    private String title;

    private String description;

    private String priority;

    private LocalDate dueDate;

    // Optional — only used at creation time to set the initial assignee without touching the audit trail
    private Long assigneeId;

}
