package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.constants.IssueTaskFieldNames;
import com.project.projectmanagementapplication.dto.IssueCountsResponse;
import com.project.projectmanagementapplication.dto.IssueDetailResponse;
import com.project.projectmanagementapplication.dto.IssueFieldChangeSnapshot;
import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.IssueSprintAssignmentRequest;
import com.project.projectmanagementapplication.dto.IssueTimelineData;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.mapper.IssueMapper;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.IssueActivity;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.CommentRepository;
import com.project.projectmanagementapplication.repository.IssueActivityRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.SprintRepository;
import com.project.projectmanagementapplication.util.IssueActivityValueFormats;
import com.project.projectmanagementapplication.util.IssueTimelineMerger;
import com.project.projectmanagementapplication.util.IssueUpdateChangeDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IssueServiceImpl implements IssueService {

    private static final int TIMELINE_DEFAULT_LIMIT = 200;
    private static final int TIMELINE_MAX_LIMIT = 500;

    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final ProjectService projectService;
    private final UserService userService;
    private final IssueActivityRecorder issueActivityRecorder;
    private final IssueActivityRepository issueActivityRepository;
    private final CommentRepository commentRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Autowired
    public IssueServiceImpl(
            IssueRepository issueRepository,
            IssueMapper issueMapper,
            ProjectService projectService,
            UserService userService,
            IssueActivityRecorder issueActivityRecorder,
            IssueActivityRepository issueActivityRepository,
            CommentRepository commentRepository,
            SprintRepository sprintRepository,
            ProjectAuthorizationService projectAuthorizationService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.projectService = projectService;
        this.userService = userService;
        this.issueActivityRecorder = issueActivityRecorder;
        this.issueActivityRepository = issueActivityRepository;
        this.commentRepository = commentRepository;
        this.sprintRepository = sprintRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Issue getIssueById(Long issueId) throws Exception {
        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isPresent()) {
            return issue.get();
        }
        throw new Exception("Issue not found with ID: " + issueId);
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<IssueResponse>> getIssueByProjectId(Long projectId) throws Exception {
        Project project;
        try {
            project = projectService.getProjectById(projectId).getData();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Project not found with Id: " + projectId);
        }

        List<Issue> issues = issueRepository.findByProjectId(projectId);

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
    @Transactional(readOnly = true)
    public Response<List<IssueResponse>> getBacklogIssues(Long projectId, User caller) throws Exception {
        Project project = loadProjectOrNotFound(projectId);
        assertScrumProject(project);
        assertProjectMember(project, caller);

        List<Issue> issues = issueRepository.findByProjectIdAndSprintIsNull(projectId);
        List<IssueResponse> issueResponses = issues.stream()
                .map(issue -> issueMapper.toIssueResponse(issue, project))
                .toList();

        String message = issues.isEmpty()
                ? "No backlog tasks for this project"
                : "Backlog issues retrieved successfully";

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
            assertUserMayCreateIssueInProject(project, user);

            Issue issue = new Issue();
            issue.setTitle(issueRequest.getTitle());
            issue.setDescription(issueRequest.getDescription());
            issue.setStatus(ISSUE_STATUS.TO_DO);
            if (issueRequest.getPriority().equals(ISSUE_PRIORITY.HIGH.toString())) {
                issue.setPriority(ISSUE_PRIORITY.HIGH);
            } else if (issueRequest.getPriority().equals(ISSUE_PRIORITY.MEDIUM.toString())) {
                issue.setPriority(ISSUE_PRIORITY.MEDIUM);
            } else {
                issue.setPriority(ISSUE_PRIORITY.LOW);
            }
            issue.setCreatedBy(user);
            issue.setAssignedBy(user);
            issue.setAssignedDate(LocalDate.now());
            issue.setDueDate(issueRequest.getDueDate());
            issue.setProject(project);
            issue.setSprint(null);

            if (issueRequest.getAssigneeId() != null) {
                User assignee = userService.findByUserId(issueRequest.getAssigneeId());
                issue.setAssignee(assignee);
            }

            Issue createdIssue = issueRepository.save(issue);
            issueActivityRecorder.recordTaskCreated(createdIssue, user);

            IssueResponse issueResponse = issueMapper.toIssueResponse(createdIssue, project);
            return Response.<IssueResponse>builder()
                    .data(issueResponse)
                    .message("Issue created successfully")
                    .status(HttpStatus.CREATED)
                    .statusCode(HttpStatus.CREATED.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();

        } catch (ResourceNotFoundException | BadRequestException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Response<IssueResponse> assignIssueSprint(Long issueId, IssueSprintAssignmentRequest request, Long userId)
            throws Exception {
        Issue issue = getIssueById(issueId);
        assertIssueNotInCompletedSprint(issue);
        Project project = issue.getProject();

        if (project.getFramework() != PROJECT_FRAMEWORK.SCRUM) {
            throw new BadRequestException("Sprint assignment is only available for Scrum projects");
        }

        User editor = userService.findByUserId(userId);
        boolean elevated =
                projectAuthorizationService.canManageSprints(project, editor)
                        || projectAuthorizationService.canAdministerAllTasks(project.getId(), editor);
        boolean canAssign =
                elevated
                        || issue.getCreatedBy().getId().equals(userId)
                        || (issue.getAssignee() != null && issue.getAssignee().getId().equals(userId));
        if (!canAssign) {
            throw new UnauthorizedException(
                    "Only an elevated role (owner, admin, or Scrum Master), the task creator, or the assignee can move this task");
        }

        String oldLabel = IssueActivityValueFormats.sprintDisplay(issue.getSprint());
        ISSUE_STATUS statusBeforeReset = issue.getStatus();

        if (request.getSprintId() == null) {
            issue.setSprint(null);
        } else {
            Sprint sprint = sprintRepository
                    .findByIdAndProject_Id(request.getSprintId(), project.getId())
                    .orElseThrow(() -> new BadRequestException("Sprint does not belong to this project"));
            if (sprint.getStatus() == SPRINT_STATUS.COMPLETED) {
                throw new BadRequestException("Cannot assign issues to a completed sprint");
            }
            if (projectAuthorizationService.mustRestrictSprintAssignmentToActive(project.getId(), editor)
                    && sprint.getStatus() != SPRINT_STATUS.ACTIVE) {
                throw new BadRequestException("Team members may only assign tasks to the active sprint");
            }
            issue.setSprint(sprint);
        }

        resetIssueToTodoClearingWorkflow(issue);

        LocalDateTime at = LocalDateTime.now();
        issue.setLastUpdatedBy(editor);
        issue.setLastUpdatedAt(at);

        Issue saved = issueRepository.save(issue);
        String newLabel = IssueActivityValueFormats.sprintDisplay(saved.getSprint());
        issueActivityRecorder.recordSingleFieldUpdate(
                saved, editor, IssueTaskFieldNames.SPRINT, oldLabel, newLabel, at);
        if (statusBeforeReset != null && statusBeforeReset != ISSUE_STATUS.TO_DO) {
            issueActivityRecorder.recordSingleFieldUpdate(
                    saved,
                    editor,
                    IssueTaskFieldNames.STATUS,
                    IssueActivityValueFormats.statusDisplay(statusBeforeReset),
                    IssueActivityValueFormats.statusDisplay(ISSUE_STATUS.TO_DO),
                    at);
        }

        IssueResponse issueResponse = issueMapper.toIssueResponse(saved, project);
        return Response.<IssueResponse>builder()
                .data(issueResponse)
                .message("Issue sprint updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<Long> deleteIssue(Long issueId, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);
        assertIssueNotInCompletedSprint(issue);

        User editor = userService.findByUserId(userId);
        boolean elevated = projectAuthorizationService.canAdministerAllTasks(issue.getProject().getId(), editor);
        boolean isCreator = issue.getCreatedBy().getId().equals(userId);
        if (!elevated && !isCreator) {
            throw new UnauthorizedException(
                    "Only the creator or a project owner/admin can delete this issue.");
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
        assertIssueNotInCompletedSprint(issue);

        User editor = userService.findByUserId(userId);
        Project project = issue.getProject();
        boolean elevated = projectAuthorizationService.canAdministerAllTasks(project.getId(), editor);
        // MEMBER assignee may change status only (updateIssueStatus); full field edit is creator or elevated.
        boolean memberMayEditFullFields = issue.getCreatedBy().getId().equals(userId);
        if (!elevated && !memberMayEditFullFields) {
            throw new UnauthorizedException("You are not authorized to update this issue");
        }


        if (issueRequest.getPriority() != null) {
            try {
                ISSUE_PRIORITY.valueOf(issueRequest.getPriority());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid priority value: " + issueRequest.getPriority());
            }
        }

        List<IssueFieldChangeSnapshot> changes = IssueUpdateChangeDetector.detect(issue, issueRequest);
        if (changes.isEmpty()) {
            IssueResponse unchanged = issueMapper.toIssueResponse(issue, project);
            return Response.<IssueResponse>builder()
                    .data(unchanged)
                    .message("Issue updated successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        issue.setTitle(issueRequest.getTitle());
        issue.setDescription(issueRequest.getDescription());

        if (issueRequest.getPriority() != null) {
            ISSUE_PRIORITY priority = ISSUE_PRIORITY.valueOf(issueRequest.getPriority());
            issue.setPriority(priority);
        }

        issue.setDueDate(issueRequest.getDueDate());

        LocalDateTime at = LocalDateTime.now();
        issue.setLastUpdatedBy(editor);
        issue.setLastUpdatedAt(at);

        Issue updatedIssue = issueRepository.save(issue);
        issueActivityRecorder.recordFieldUpdates(updatedIssue, editor, changes, at);

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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Response<IssueTimelineData> getIssueTimeline(Long issueId, int limit) throws Exception {
        getIssueById(issueId);
        int safeLimit = Math.min(Math.max(limit <= 0 ? TIMELINE_DEFAULT_LIMIT : limit, 1), TIMELINE_MAX_LIMIT);
        int fetchWindow = Math.min(safeLimit * 2, TIMELINE_MAX_LIMIT * 2);

        List<IssueActivity> activities = issueActivityRepository
                .findByIssue_IdOrderByCreatedAtDesc(issueId, PageRequest.of(0, fetchWindow))
                .getContent();
        List<Comment> comments = commentRepository
                .findByIssue_IdOrderByCreatedDateTimeDesc(issueId, PageRequest.of(0, fetchWindow))
                .getContent();

        var merged = IssueTimelineMerger.merge(activities, comments, safeLimit);
        IssueTimelineData data = IssueTimelineData.builder()
                .items(merged)
                .limit(safeLimit)
                .build();

        return Response.<IssueTimelineData>builder()
                .data(data)
                .message("Issue timeline retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> addUserToIssue(Long issueId, Long assigneeUserId, Long callerId) throws Exception {
        Issue issue = getIssueById(issueId);
        assertIssueNotInCompletedSprint(issue);
        Project project = issue.getProject();

        User callerUser = userService.findByUserId(callerId);
        boolean elevated = projectAuthorizationService.canAdministerAllTasks(project.getId(), callerUser);
        boolean isCreator = issue.getCreatedBy().getId().equals(callerId);
        if (!elevated && !isCreator) {
            throw new UnauthorizedException("Only the issue creator or project owner can assign this issue.");
        }

        if (issue.getAssignee() != null && issue.getAssignee().getId().equals(assigneeUserId)) {
            IssueResponse same = issueMapper.toIssueResponse(issue, project);
            return Response.<IssueResponse>builder()
                    .data(same)
                    .message("Assignee updated successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        User assignee = userService.findByUserId(assigneeUserId);

        String oldAssigneeName = IssueActivityValueFormats.assigneeDisplay(issue.getAssignee());
        String newAssigneeName = IssueActivityValueFormats.assigneeDisplay(assignee);

        issue.setAssignee(assignee);
        issue.setAssignedBy(callerUser);
        LocalDateTime at = LocalDateTime.now();
        issue.setLastUpdatedBy(callerUser);
        issue.setLastUpdatedAt(at);

        Issue updatedIssue = issueRepository.save(issue);
        issueActivityRecorder.recordSingleFieldUpdate(
                updatedIssue, callerUser, IssueTaskFieldNames.ASSIGNEE, oldAssigneeName, newAssigneeName, at);

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
    public Response<IssueResponse> removeAssigneeFromIssue(Long issueId, Long callerId) throws Exception {
        Issue issue = getIssueById(issueId);
        assertIssueNotInCompletedSprint(issue);
        Project project = issue.getProject();

        User callerUser = userService.findByUserId(callerId);
        boolean elevated = projectAuthorizationService.canAdministerAllTasks(project.getId(), callerUser);
        boolean isCreator = issue.getCreatedBy().getId().equals(callerId);
        if (!elevated && !isCreator) {
            throw new UnauthorizedException("Only the issue creator or project owner can remove the assignee.");
        }

        if (issue.getAssignee() == null) {
            IssueResponse same = issueMapper.toIssueResponse(issue, project);
            return Response.<IssueResponse>builder()
                    .data(same)
                    .message("Assignee removed successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        String oldAssigneeName = IssueActivityValueFormats.assigneeDisplay(issue.getAssignee());

        issue.setAssignee(null);
        issue.setAssignedBy(callerUser);
        LocalDateTime at = LocalDateTime.now();
        issue.setLastUpdatedBy(callerUser);
        issue.setLastUpdatedAt(at);

        Issue updatedIssue = issueRepository.save(issue);
        issueActivityRecorder.recordSingleFieldUpdate(
                updatedIssue, callerUser, IssueTaskFieldNames.ASSIGNEE, oldAssigneeName, "Unassigned", at);

        IssueResponse issueResponse = issueMapper.toIssueResponse(updatedIssue, project);
        return Response.<IssueResponse>builder()
                .data(issueResponse)
                .message("Assignee removed successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<IssueResponse> updateIssueStatus(Long issueId, String status, Long userId) throws Exception {
        Issue issue = getIssueById(issueId);
        assertIssueNotInCompletedSprint(issue);
        Project project = issue.getProject();

        boolean canUpdateStatus = false;

        User editor = userService.findByUserId(userId);
        if (issue.getAssignee() != null && issue.getAssignee().getId().equals(userId)) {
            canUpdateStatus = true;
        } else if (issue.getCreatedBy().getId().equals(userId)) {
            canUpdateStatus = true;
        } else if (projectAuthorizationService.canActAsProjectAdminForIssue(editor, project)) {
            canUpdateStatus = true;
        }

        if (!canUpdateStatus) {
            throw new UnauthorizedException("Only the assignee, issue creator, or project owner can update the issue status");
        }

        ISSUE_STATUS target;
        try {
            target = ISSUE_STATUS.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }

        if (project.getFramework() == PROJECT_FRAMEWORK.SCRUM) {
            Sprint sprint = issue.getSprint();
            boolean inActiveSprint =
                    sprint != null && sprint.getStatus() == SPRINT_STATUS.ACTIVE;
            if (!inActiveSprint && target != ISSUE_STATUS.TO_DO) {
                throw new BadRequestException(
                        "In Scrum, task status can only move to In Progress or Done when the sprint is active");
            }
        }

        if (issue.getStatus() == target) {
            IssueResponse same = issueMapper.toIssueResponse(issue, project);
            return Response.<IssueResponse>builder()
                    .data(same)
                    .message("Issue status updated successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        ISSUE_STATUS oldStatus = issue.getStatus();

        if (status.equals(ISSUE_STATUS.TO_DO.toString())) {
            issue.setStatus(ISSUE_STATUS.TO_DO);
            issue.setTaskCompletedAt(null);
        } else if (status.equals(ISSUE_STATUS.IN_PROGRESS.toString())) {
            issue.setStatus(ISSUE_STATUS.IN_PROGRESS);
            if (issue.getTaskStartedAt() == null) {
                issue.setTaskStartedAt(LocalDateTime.now());
            }
            issue.setTaskCompletedAt(null);
        } else if (status.equals(ISSUE_STATUS.DONE.toString())) {
            issue.setStatus(ISSUE_STATUS.DONE);
            issue.setTaskCompletedAt(LocalDateTime.now());
        } else {
            throw new BadRequestException("Invalid status: " + status);
        }

        LocalDateTime at = LocalDateTime.now();
        issue.setLastUpdatedBy(editor);
        issue.setLastUpdatedAt(at);

        Issue updatedIssue = issueRepository.save(issue);
        issueActivityRecorder.recordSingleFieldUpdate(
                updatedIssue,
                editor,
                IssueTaskFieldNames.STATUS,
                IssueActivityValueFormats.statusDisplay(oldStatus),
                IssueActivityValueFormats.statusDisplay(target),
                at);

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
    @Transactional(readOnly = true)
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

    private static void resetIssueToTodoClearingWorkflow(Issue issue) {
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setTaskCompletedAt(null);
        issue.setTaskStartedAt(null);
    }

    private Project loadProjectOrNotFound(Long projectId) throws Exception {
        try {
            return projectService.getProjectById(projectId).getData();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Project not found with Id: " + projectId);
        }
    }

    private void assertScrumProject(Project project) {
        if (project.getFramework() != PROJECT_FRAMEWORK.SCRUM) {
            throw new BadRequestException("This operation is only available for Scrum projects");
        }
    }

    private void assertProjectMember(Project project, User user) {
        if (project.getOwner().getId().equals(user.getId())) {
            return;
        }
        boolean inTeam = project.getTeam() != null
                && project.getTeam().stream().anyMatch(m -> m.getId().equals(user.getId()));
        if (!inTeam) {
            throw new UnauthorizedException("You are not a member of this project");
        }
    }

    private void assertUserMayCreateIssueInProject(Project project, User user) {
        assertProjectMember(project, user);
    }

    /**
     * Scrum: issues in a {@link SPRINT_STATUS#COMPLETED} sprint are read-only (no field, status,
     * assignee, sprint moves, or delete).
     */
    private void assertIssueNotInCompletedSprint(Issue issue) {
        Sprint sprint = issue.getSprint();
        if (sprint != null && sprint.getStatus() == SPRINT_STATUS.COMPLETED) {
            throw new BadRequestException("Cannot modify tasks in a completed sprint");
        }
    }
}
