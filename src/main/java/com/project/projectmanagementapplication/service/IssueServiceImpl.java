package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.IssueCountsResponse;
import com.project.projectmanagementapplication.dto.IssueDetailResponse;
import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.mapper.IssueMapper;
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
    private final IssueMapper issueMapper;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public IssueServiceImpl(IssueRepository issueRepository, IssueMapper issueMapper, ProjectService projectService, UserService userService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
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
        // First verify project exists and get project details
        Project project;
        try {
            project = projectService.getProjectById(projectId).getData();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Project not found with Id: " + projectId);
        }

        List<Issue> issues = issueRepository.findByProjectId(projectId);

        // Handle empty issues list - this is valid, not an error
        List<IssueResponse> issueResponses = issues.stream()
                .map(issue -> issueMapper.toIssueResponse(issue, project))
                .toList();

        String message = issues.isEmpty() ?
                "No issues found for this project" :
                "Issues retrieved successfully";

        return Response.<List<IssueResponse>>builder()
                .data(issueResponses)
                .message(message)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> createIssue(Long projectId, IssueRequest issueRequest, User user) {
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

            // If an assignee was selected at creation time, set it now without touching the audit trail.
            // lastEditedBy/lastEditedAt are intentionally left null — they only reflect post-creation edits.
            if (issueRequest.getAssigneeId() != null) {
                User assignee = userService.findByUserId(issueRequest.getAssigneeId());
                issue.setAssignee(assignee);
                issue.setAssignedBy(user);
            }

            Issue createdIssue = issueRepository.save(issue);

            IssueResponse issueResponse = issueMapper.toIssueResponse(createdIssue, project);
            return Response.<IssueResponse>builder()
                    .data(issueResponse)
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

        boolean isCreator = issue.getCreatedBy().getId().equals(userId);
        boolean isProjectOwner = issue.getProject().getOwner().getId().equals(userId);
        if (!isCreator && !isProjectOwner) {
            throw new UnauthorizedException("Only the creator or project owner can delete this issue.");
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

        boolean isCreator = issue.getCreatedBy().getId().equals(userId);
        boolean isProjectOwner = issue.getProject().getOwner().getId().equals(userId);
        if (!isCreator && !isProjectOwner) {
            throw new UnauthorizedException("You are not authorized to update this issue");
        }

        // Get project for projectOwnerId
        Project project = issue.getProject();

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

        User editor = userService.findByUserId(userId);
        issue.setLastEditedBy(editor);
        issue.setLastEditedAt(LocalDateTime.now());

        Issue updatedIssue = issueRepository.save(issue);

        IssueResponse issueResponse = issueMapper.toIssueResponse(updatedIssue, project);

        return Response.<IssueResponse>builder()
                .data(issueResponse)
                .message("Issue updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }


    @Override
    public Response<IssueDetailResponse> getIssueDetail(Long issueId) throws Exception {
        Issue issue = getIssueById(issueId);

        IssueDetailResponse issueDetailResponse = issueMapper.toIssueDetailResponse(issue);

        return Response.<IssueDetailResponse>builder()
                .data(issueDetailResponse)
                .message("Issue details retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> addUserToIssue(Long issueId, Long assigneeUserId, Long callerId) throws Exception {
        Issue issue = getIssueById(issueId);
        Project project = issue.getProject();

        boolean isCreator      = issue.getCreatedBy().getId().equals(callerId);
        boolean isProjectOwner = project.getOwner().getId().equals(callerId);
        if (!isCreator && !isProjectOwner) {
            throw new UnauthorizedException("Only the issue creator or project owner can assign this issue.");
        }

        User assignee = userService.findByUserId(assigneeUserId);
        User caller   = userService.findByUserId(callerId);

        issue.setAssignee(assignee);
        issue.setAssignedBy(caller);
        issue.setLastEditedBy(caller);
        issue.setLastEditedAt(LocalDateTime.now());

        Issue updatedIssue = issueRepository.save(issue);
        IssueResponse issueResponse = issueMapper.toIssueResponse(updatedIssue, project);
        return Response.<IssueResponse>builder()
                .data(issueResponse)
                .message("Assignee updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> updateIssueStatus(Long issueId, String status, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);
        Project project = issue.getProject();

        //Allow assignee, issue creator, or project owner to update issue status
        boolean canUpdateStatus = false;

        // Check if user is the assignee
        if (issue.getAssignee() != null && issue.getAssignee().getId().equals(userId)) {
            canUpdateStatus = true;
        }
        // Check if user is the issue creator
        else if (issue.getCreatedBy().getId().equals(userId)) {
            canUpdateStatus = true;
        }
        // Check if user is the project owner
        else if (project.getOwner().getId().equals(userId)) {
            canUpdateStatus = true;
        }

        if (!canUpdateStatus) {
            throw new UnauthorizedException("Only the assignee, issue creator, or project owner can update the issue status");
        }

        // Validate, set status, and record workflow timestamps for Kanban metrics
        if (status.equals(ISSUE_STATUS.TO_DO.toString())) {
            issue.setStatus(ISSUE_STATUS.TO_DO);
            // Task reopened — clear completion timestamp; taskStartedAt is preserved intentionally
            issue.setTaskCompletedAt(null);
        }
        else if (status.equals(ISSUE_STATUS.IN_PROGRESS.toString())) {
            issue.setStatus(ISSUE_STATUS.IN_PROGRESS);
            // Record when work first started — never overwrite once set
            if (issue.getTaskStartedAt() == null) {
                issue.setTaskStartedAt(LocalDateTime.now());
            }
            // Task moved back from DONE — clear completion timestamp
            issue.setTaskCompletedAt(null);
        }
        else if (status.equals(ISSUE_STATUS.DONE.toString())) {
            issue.setStatus(ISSUE_STATUS.DONE);
            issue.setTaskCompletedAt(LocalDateTime.now());
        }
        else {
            throw new BadRequestException("Invalid status: " + status);
        }

        Issue updatedIssue = issueRepository.save(issue);
        IssueResponse issueResponse = issueMapper.toIssueResponse(updatedIssue, project);
        return Response.<IssueResponse>builder()
                .data(issueResponse)
                .message("Issue status updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueCountsResponse> getIssueCountsForUser(User user) throws Exception {
        List<Project> relevantProjects = projectService.getAllProjectForUser(user, null, null).getData();

        long assignedTasks = issueRepository.countByAssigneeAndProjectIn(user, relevantProjects);
        long overdueTasks = issueRepository
                .countByAssigneeAndProjectInAndDueDateBeforeAndStatusNot(
                        user,
                        relevantProjects,
                        LocalDate.now(),
                        ISSUE_STATUS.DONE);

        long dueTodayTasks = issueRepository
                .countByAssigneeAndProjectInAndDueDateAndStatusNot(
                        user,
                        relevantProjects,
                        LocalDate.now(),
                        ISSUE_STATUS.DONE);

        long highPriorityTasks = issueRepository
                .countByAssigneeAndProjectInAndPriorityAndStatusNot(
                        user,
                        relevantProjects,
                        ISSUE_PRIORITY.HIGH,
                        ISSUE_STATUS.DONE);

        long completedTasks = issueRepository
                .countByAssigneeAndProjectInAndStatus(
                        user,
                        relevantProjects,
                        ISSUE_STATUS.DONE);

        IssueCountsResponse counts = IssueCountsResponse.builder()
                .assignedTasks(assignedTasks)
                .overdueTasks(overdueTasks)
                .dueTodayTasks(dueTodayTasks)
                .highPriorityTasks(highPriorityTasks)
                .completedTasks(completedTasks)
                .build();

        return Response.<IssueCountsResponse>builder()
                .data(counts)
                .message("Issue counts retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}