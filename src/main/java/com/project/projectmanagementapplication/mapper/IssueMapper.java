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
                .commentCount(issue.getComments() != null ? issue.getComments().size() : 0)
                .lastEditedByName(issue.getLastEditedBy() != null ? fullName(issue.getLastEditedBy()) : null)
                .lastEditedAt(issue.getLastEditedAt())
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
                .comments(issue.getComments() != null
                        ? issue.getComments().stream().map(commentMapper::toCommentResponse).toList()
                        : List.of())
                .lastEditedByName(issue.getLastEditedBy() != null ? fullName(issue.getLastEditedBy()) : null)
                .lastEditedAt(issue.getLastEditedAt())
                .build();
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
