package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IssueServiceImpl implements IssueService {


    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public IssueServiceImpl(IssueRepository issueRepository, ProjectService projectService, UserService userService) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Override
    public Issue getIssueById(Long issueId) throws Exception {
        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isPresent()) {
            return issue.get();
        }
        throw new Exception("Issue not found with ID: " + issueId);
    }

    @Override
    public Response<List<IssueResponse>> getIssueByProjectId(Long projectId) throws Exception {
        List<Issue> issues = issueRepository.findByProjectId(projectId);
        if (issues.isEmpty()) {
            throw new ResourceNotFoundException("No issues found for project with Id: " + projectId);
        }

        List<IssueResponse> issueResponses = issues.stream().map(issue -> IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus().toString())
                .priority(issue.getPriority().toString())
                .assignedDate(issue.getAssignedDate())
                .dueDate(issue.getDueDate())
                .createdById(issue.getCreatedBy().getId())
                .assignee(issue.getAssignee())
                .projectId(issue.getProject() != null ? issue.getProject().getId() : null)
                .project(null)
                .comments(issue.getComments())
                .build()).toList();

        return Response.<List<IssueResponse>>builder()
                .data(issueResponses)
                .message("Issues retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }


    @Override
    public Response<IssueResponse> createIssue(Long projectId,IssueRequest issueRequest, User user)  {
        try {
            Project project = projectService.getProjectById(projectId).getData();
            Issue issue = new Issue();
            issue.setTitle(issueRequest.getTitle());
            issue.setDescription(issueRequest.getDescription());
            issue.setStatus(ISSUE_STATUS.TO_DO);
            if(issueRequest.getPriority().equals(ISSUE_PRIORITY.HIGH.toString())) {
                issue.setPriority(ISSUE_PRIORITY.HIGH);
            }
            else if(issueRequest.getPriority().equals(ISSUE_PRIORITY.MEDIUM.toString())) {
                issue.setPriority(ISSUE_PRIORITY.MEDIUM);

            } else {
                issue.setPriority(ISSUE_PRIORITY.LOW);
            }
            issue.setCreatedBy(user);
            issue.setAssignedDate(LocalDate.now());
            issue.setDueDate(issueRequest.getDueDate());
            issue.setProject(project);
            Issue createdIssue = issueRepository.save(issue);

            return Response.<IssueResponse>builder()
                    .data(IssueResponse.builder()
                            .id(createdIssue.getId())
                            .title(createdIssue.getTitle())
                            .description(createdIssue.getDescription())
                            .status(createdIssue.getStatus().toString())
                            .priority(createdIssue.getPriority().toString())
                            .assignedDate(createdIssue.getAssignedDate())
                            .dueDate(createdIssue.getDueDate())
                            .createdById(createdIssue.getCreatedBy().getId())
                            .assignee(createdIssue.getAssignee())
                            .projectId(createdIssue.getProject().getId())
                            .comments(createdIssue.getComments())
                            .build())
                    .message("Issue created successfully")
                    .status(HttpStatus.CREATED)
                    .statusCode(HttpStatus.CREATED.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Response<Long> deleteIssue(Long issueId, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);

        if (!issue.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedException("Only the creator can delete this issue.");
        }

        issueRepository.deleteById(issue.getId());

        return Response.<Long>builder()
                .data(issue.getCreatedBy().getId())
                .message("Issue deleted successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }



    @Override
    public Response<IssueResponse> updateIssue(Long issueId, IssueRequest issueRequest, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);

        if (!issue.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this issue");
        }

        issue.setTitle(issueRequest.getTitle());
        issue.setDescription(issueRequest.getDescription());

        if (issueRequest.getPriority() != null) {
            try {
                ISSUE_PRIORITY priority = ISSUE_PRIORITY.valueOf(issueRequest.getPriority());
                issue.setPriority(priority);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid priority value: " + issueRequest.getPriority());
            }
        }

        issue.setDueDate(issueRequest.getDueDate());

        Issue updatedIssue = issueRepository.save(issue);

        IssueResponse responseData = IssueResponse.builder()
                .id(updatedIssue.getId())
                .title(updatedIssue.getTitle())
                .description(updatedIssue.getDescription())
                .status(updatedIssue.getStatus().toString())
                .priority(updatedIssue.getPriority().toString())
                .assignedDate(updatedIssue.getAssignedDate())
                .dueDate(updatedIssue.getDueDate())
                .createdById(issue.getCreatedBy().getId())
                .assignee(updatedIssue.getAssignee())
                .projectId(updatedIssue.getProject() != null ? updatedIssue.getProject().getId() : null)
                .createdById(updatedIssue.getCreatedBy().getId())
                .comments(updatedIssue.getComments())
                .build();

        return Response.<IssueResponse>builder()
                .data(responseData)
                .message("Issue updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> addUserToIssue(Long issueId, Long userId) throws Exception {
        User user = userService.findByUserId(userId);
        Issue issue = getIssueById(issueId);
        issue.setAssignee(user);
        Issue updatedIssue = issueRepository.save(issue);
        return Response.<IssueResponse>builder()
                .data(IssueResponse.builder()
                        .id(updatedIssue.getId())
                        .title(updatedIssue.getTitle())
                        .description(updatedIssue.getDescription())
                        .status(updatedIssue.getStatus().toString())
                        .priority(updatedIssue.getPriority().toString())
                        .assignedDate(updatedIssue.getAssignedDate())
                        .dueDate(updatedIssue.getDueDate())
                        .projectId(updatedIssue.getProject().getId())
                        .assignee(updatedIssue.getAssignee())
                        .comments(updatedIssue.getComments())
                        .build())
                .message("User added to issue successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> updateIssueStatus(Long issueId, String status, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);
        if (issue.getAssignee() != null && !issue.getAssignee().getId().equals(userId)) {
            throw new Exception("You are not authorized to update this issue"); //only the assignee can update the issue status
        }
        else if(status.equals(ISSUE_STATUS.TO_DO.toString())){
            issue.setStatus(ISSUE_STATUS.TO_DO);
        }
        else if(status.equals(ISSUE_STATUS.IN_PROGRESS.toString())){
            issue.setStatus(ISSUE_STATUS.IN_PROGRESS);
        }
        else if(status.equals(ISSUE_STATUS.DONE.toString())){
            issue.setStatus(ISSUE_STATUS.DONE);
        }
        else {
            throw new BadRequestException("Invalid status: " + status);
        }

        Issue updatedIssue =  issueRepository.save(issue);
        return Response.<IssueResponse>builder()
                .data(IssueResponse.builder()
                        .id(updatedIssue.getId())
                        .title(updatedIssue.getTitle())
                        .description(updatedIssue.getDescription())
                        .status(updatedIssue.getStatus().toString())
                        .priority(updatedIssue.getPriority().toString())
                        .assignedDate(updatedIssue.getAssignedDate())
                        .dueDate(updatedIssue.getDueDate())
                        .projectId(updatedIssue.getProject().getId())
                        .createdById(updatedIssue.getCreatedBy().getId())
                        .assignee(updatedIssue.getAssignee())
                        .comments(updatedIssue.getComments())
                        .build())
                .message("Issue status updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }


}
