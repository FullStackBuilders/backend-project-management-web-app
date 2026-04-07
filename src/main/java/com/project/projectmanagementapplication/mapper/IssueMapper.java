package com.project.projectmanagementapplication.mapper;

import com.project.projectmanagementapplication.dto.IssueDetailResponse;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IssueMapper {

    private final CommentMapper commentMapper;

    public IssueMapper(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public IssueResponse toIssueResponse(Issue issue, Project project) {
        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus().toString())
                .priority(issue.getPriority().toString())
                .assignedDate(issue.getAssignedDate())
                .dueDate(issue.getDueDate())
                .createdById(issue.getCreatedBy().getId())
                .createdByName(fullName(issue.getCreatedBy()))
                .assigneeId(issue.getAssignee() != null ? issue.getAssignee().getId() : null)
                .assigneeName(issue.getAssignee() != null ? fullName(issue.getAssignee()) : null)
                .assignedById(issue.getAssignedBy() != null ? issue.getAssignedBy().getId() : null)
                .assignedByName(issue.getAssignedBy() != null ? fullName(issue.getAssignedBy()) : null)
                .projectId(issue.getProject() != null ? issue.getProject().getId() : null)
                .projectOwnerId(project.getOwner().getId())
                .sprintId(issue.getSprint() != null ? issue.getSprint().getId() : null)
                .sprintName(issue.getSprint() != null ? issue.getSprint().getName() : null)
                .commentCount(issue.getComments() != null ? issue.getComments().size() : 0)
                .lastUpdatedByName(issue.getLastUpdatedBy() != null ? fullName(issue.getLastUpdatedBy()) : null)
                .lastUpdatedAt(issue.getLastUpdatedAt())
                .taskCreatedAt(issue.getCreatedAt())
                .taskStartedAt(issue.getTaskStartedAt())
                .taskCompletedAt(issue.getTaskCompletedAt())
                .build();
    }

    public IssueDetailResponse toIssueDetailResponse(Issue issue) {
        Project project = issue.getProject();

        return IssueDetailResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus().toString())
                .priority(issue.getPriority().toString())
                .assignedDate(issue.getAssignedDate())
                .dueDate(issue.getDueDate())
                .createdById(issue.getCreatedBy().getId())
                .createdByName(fullName(issue.getCreatedBy()))
                .assigneeId(issue.getAssignee() != null ? issue.getAssignee().getId() : null)
                .assigneeName(issue.getAssignee() != null ? fullName(issue.getAssignee()) : null)
                .assignedById(issue.getAssignedBy() != null ? issue.getAssignedBy().getId() : null)
                .assignedByName(issue.getAssignedBy() != null ? fullName(issue.getAssignedBy()) : null)
                .projectId(project.getId())
                .projectName(project.getName())
                .projectOwnerId(project.getOwner().getId())
                .projectOwnerName(fullName(project.getOwner()))
                .sprintId(issue.getSprint() != null ? issue.getSprint().getId() : null)
                .sprintName(issue.getSprint() != null ? issue.getSprint().getName() : null)
                .comments(issue.getComments() != null
                        ? issue.getComments().stream().map(commentMapper::toCommentResponse).toList()
                        : List.of())
                .lastUpdatedByName(issue.getLastUpdatedBy() != null ? fullName(issue.getLastUpdatedBy()) : null)
                .lastUpdatedAt(issue.getLastUpdatedAt())
                .build();
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
