package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.constants.IssueTaskFieldNames;
import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.IssueSprintAssignmentRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.mapper.IssueMapper;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.CommentRepository;
import com.project.projectmanagementapplication.repository.IssueActivityRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @InjectMocks
    private IssueServiceImpl issueService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private IssueActivityRecorder issueActivityRecorder;

    @Mock
    private IssueActivityRepository issueActivityRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @BeforeEach
    void stubProjectAuth() {
        lenient()
                .when(projectAuthorizationService.canManageSprints(any(Project.class), any(User.class)))
                .thenAnswer(
                        inv -> {
                            Project p = inv.getArgument(0);
                            User u = inv.getArgument(1);
                            return p.getOwner() != null && p.getOwner().getId().equals(u.getId());
                        });
        lenient()
                .when(projectAuthorizationService.canAdministerAllTasks(any(Long.class), any(User.class)))
                .thenReturn(false);
        lenient()
                .when(
                        projectAuthorizationService.mustRestrictSprintAssignmentToActive(
                                any(Long.class), any(User.class)))
                .thenReturn(false);
        lenient()
                .when(
                        projectAuthorizationService.canActAsProjectAdminForIssue(
                                any(User.class), any(Project.class)))
                .thenAnswer(
                        inv -> {
                            User u = inv.getArgument(0);
                            Project p = inv.getArgument(1);
                            return p.getOwner() != null && p.getOwner().getId().equals(u.getId());
                        });
    }

    @Test
    void updateIssueStatus_setsLastUpdatedByAndLastUpdatedAt() throws Exception {
        Long issueId = 10L;
        Long assigneeUserId = 2L;

        User assignee = new User();
        assignee.setId(assigneeUserId);
        assignee.setFirstName("Alex");
        assignee.setLastName("Assignee");

        User owner = new User();
        owner.setId(100L);
        User creator = new User();
        creator.setId(50L);

        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(assignee);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(assigneeUserId)).thenReturn(assignee);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        Response<IssueResponse> response =
                issueService.updateIssueStatus(issueId, ISSUE_STATUS.IN_PROGRESS.toString(), assigneeUserId);

        assertNotNull(response);
        assertNotNull(response.getData());

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        Issue saved = savedCaptor.getValue();

        assertSame(assignee, saved.getLastUpdatedBy());
        assertNotNull(saved.getLastUpdatedAt());
        assertEquals(ISSUE_STATUS.IN_PROGRESS, saved.getStatus());

        verify(issueActivityRecorder).recordSingleFieldUpdate(
                eq(saved),
                eq(assignee),
                eq(IssueTaskFieldNames.STATUS),
                eq("To Do"),
                eq("In Progress"),
                any());
    }

    @Test
    void updateIssueStatus_sameStatus_doesNotSaveOrRecord() throws Exception {
        Long issueId = 10L;
        Long userId = 2L;

        User user = new User();
        user.setId(userId);
        User owner = new User();
        owner.setId(100L);
        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(user);
        issue.setAssignee(user);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateIssueStatus(issueId, ISSUE_STATUS.TO_DO.toString(), userId);

        verify(issueRepository, never()).save(any());
        verify(issueActivityRecorder, never()).recordSingleFieldUpdate(any(), any(), anyString(), any(), any(), any());
    }

    @Test
    void removeAssigneeFromIssue_clearsAssigneeAndSetsAudit() throws Exception {
        Long issueId = 11L;
        Long ownerId = 100L;

        User owner = new User();
        owner.setId(ownerId);
        owner.setFirstName("Owner");
        owner.setLastName("User");
        User assignee = new User();
        assignee.setId(2L);
        assignee.setFirstName("Scarlett");
        assignee.setLastName("Right");
        User creator = new User();
        creator.setId(ownerId);

        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(assignee);
        issue.setAssignedBy(owner);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(ownerId)).thenReturn(owner);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        Response<IssueResponse> response = issueService.removeAssigneeFromIssue(issueId, ownerId);

        assertNotNull(response.getData());

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        Issue saved = savedCaptor.getValue();

        assertNull(saved.getAssignee());
        assertSame(owner, saved.getAssignedBy());
        assertSame(owner, saved.getLastUpdatedBy());
        assertNotNull(saved.getLastUpdatedAt());

        verify(issueActivityRecorder).recordSingleFieldUpdate(
                eq(saved),
                eq(owner),
                eq(IssueTaskFieldNames.ASSIGNEE),
                eq("Scarlett Right"),
                eq("Unassigned"),
                any());
    }

    @Test
    void getIssueTimeline_mergesEmptySources() throws Exception {
        Long issueId = 5L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("T");

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(issueActivityRepository.findByIssue_IdOrderByCreatedAtDesc(eq(issueId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(commentRepository.findByIssue_IdOrderByCreatedDateTimeDesc(eq(issueId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        var response = issueService.getIssueTimeline(issueId, 50);

        assertNotNull(response.getData());
        assertTrue(response.getData().getItems().isEmpty());
        assertEquals(50, response.getData().getLimit());
    }

    @Test
    void getBacklogIssues_nonScrum_throwsBadRequest() throws Exception {
        Long projectId = 1L;
        User caller = new User();
        caller.setId(10L);
        Project project = new Project();
        project.setId(projectId);
        project.setFramework(PROJECT_FRAMEWORK.KANBAN);
        project.setOwner(caller);

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());

        assertThrows(BadRequestException.class, () -> issueService.getBacklogIssues(projectId, caller));
    }

    @Test
    void getBacklogIssues_nonMember_throwsUnauthorized() throws Exception {
        Long projectId = 1L;
        User owner = new User();
        owner.setId(1L);
        User caller = new User();
        caller.setId(99L);
        Project project = new Project();
        project.setId(projectId);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);
        project.setTeam(Collections.emptyList());

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());

        assertThrows(UnauthorizedException.class, () -> issueService.getBacklogIssues(projectId, caller));
    }

    @Test
    void getBacklogIssues_scrumMember_queriesBacklogOnly() throws Exception {
        Long projectId = 1L;
        User member = new User();
        member.setId(5L);
        Project project = new Project();
        project.setId(projectId);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(member);

        Issue backlogIssue = new Issue();
        backlogIssue.setId(20L);
        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(issueRepository.findByProjectIdAndSprintIsNull(projectId)).thenReturn(List.of(backlogIssue));
        when(issueMapper.toIssueResponse(backlogIssue, project))
                .thenReturn(IssueResponse.builder().id(20L).build());

        var response = issueService.getBacklogIssues(projectId, member);

        assertEquals(1, response.getData().size());
        verify(issueRepository).findByProjectIdAndSprintIsNull(projectId);
    }

    @Test
    void assignIssueSprint_ownerMovesToSprint_savesAndRecordsActivity() throws Exception {
        Long issueId = 7L;
        Long ownerId = 1L;
        Long sprintId = 50L;

        User owner = new User();
        owner.setId(ownerId);
        Project project = new Project();
        project.setId(3L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setName("Sprint A");
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setCreatedBy(owner);
        issue.setSprint(null);

        IssueSprintAssignmentRequest req = new IssueSprintAssignmentRequest();
        req.setSprintId(sprintId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(sprintRepository.findByIdAndProject_Id(sprintId, project.getId())).thenReturn(Optional.of(sprint));
        when(userService.findByUserId(ownerId)).thenReturn(owner);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), eq(project)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.assignIssueSprint(issueId, req, ownerId);

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        assertSame(sprint, savedCaptor.getValue().getSprint());
        assertEquals(ISSUE_STATUS.TO_DO, savedCaptor.getValue().getStatus());

        verify(issueActivityRecorder).recordSingleFieldUpdate(
                any(Issue.class),
                eq(owner),
                eq(IssueTaskFieldNames.SPRINT),
                eq("Backlog"),
                eq("Sprint A"),
                any());
    }

    @Test
    void assignIssueSprint_completedSprint_throwsBadRequest() {
        Long issueId = 7L;
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        Project project = new Project();
        project.setId(3L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(user);

        Sprint sprint = new Sprint();
        sprint.setId(50L);
        sprint.setStatus(SPRINT_STATUS.COMPLETED);
        sprint.setProject(project);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setCreatedBy(user);

        IssueSprintAssignmentRequest req = new IssueSprintAssignmentRequest();
        req.setSprintId(50L);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(sprintRepository.findByIdAndProject_Id(50L, project.getId())).thenReturn(Optional.of(sprint));

        assertThrows(BadRequestException.class, () -> issueService.assignIssueSprint(issueId, req, userId));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void assignIssueSprint_fromInProgress_resetsToTodoAndRecordsStatus() throws Exception {
        Long issueId = 7L;
        Long ownerId = 1L;
        Long sprintId = 50L;

        User owner = new User();
        owner.setId(ownerId);
        Project project = new Project();
        project.setId(3L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setName("Sprint A");
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setCreatedBy(owner);
        issue.setSprint(null);
        issue.setStatus(ISSUE_STATUS.IN_PROGRESS);

        IssueSprintAssignmentRequest req = new IssueSprintAssignmentRequest();
        req.setSprintId(sprintId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(sprintRepository.findByIdAndProject_Id(sprintId, project.getId())).thenReturn(Optional.of(sprint));
        when(userService.findByUserId(ownerId)).thenReturn(owner);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), eq(project)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.assignIssueSprint(issueId, req, ownerId);

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        assertEquals(ISSUE_STATUS.TO_DO, savedCaptor.getValue().getStatus());

        verify(issueActivityRecorder, times(2)).recordSingleFieldUpdate(
                any(Issue.class), eq(owner), any(), any(), any(), any());
    }

    @Test
    void assignIssueSprint_moveToBacklog_resetsToTodo() throws Exception {
        Long issueId = 7L;
        Long ownerId = 1L;

        User owner = new User();
        owner.setId(ownerId);
        Project project = new Project();
        project.setId(3L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Sprint sprint = new Sprint();
        sprint.setId(50L);
        sprint.setStatus(SPRINT_STATUS.ACTIVE);
        sprint.setProject(project);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setCreatedBy(owner);
        issue.setSprint(sprint);
        issue.setStatus(ISSUE_STATUS.DONE);

        IssueSprintAssignmentRequest req = new IssueSprintAssignmentRequest();
        req.setSprintId(null);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(ownerId)).thenReturn(owner);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), eq(project)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.assignIssueSprint(issueId, req, ownerId);

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        assertNull(savedCaptor.getValue().getSprint());
        assertEquals(ISSUE_STATUS.TO_DO, savedCaptor.getValue().getStatus());
    }

    @Test
    void updateIssueStatus_scrumBacklog_cannotMoveToInProgress() throws Exception {
        Long issueId = 10L;
        Long userId = 2L;

        User u = new User();
        u.setId(userId);
        User owner = new User();
        owner.setId(100L);
        User creator = new User();
        creator.setId(50L);

        Project project = new Project();
        project.setId(1L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(u);
        issue.setSprint(null);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        assertThrows(
                BadRequestException.class,
                () -> issueService.updateIssueStatus(issueId, ISSUE_STATUS.IN_PROGRESS.toString(), userId));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void updateIssueStatus_scrumActiveSprint_allowsInProgress() throws Exception {
        Long issueId = 10L;
        Long userId = 2L;

        User u = new User();
        u.setId(userId);
        User owner = new User();
        owner.setId(100L);
        User creator = new User();
        creator.setId(50L);

        Project project = new Project();
        project.setId(1L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Sprint sprint = new Sprint();
        sprint.setId(5L);
        sprint.setStatus(SPRINT_STATUS.ACTIVE);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(u);
        issue.setSprint(sprint);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(userId)).thenReturn(u);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateIssueStatus(issueId, ISSUE_STATUS.IN_PROGRESS.toString(), userId);

        verify(issueRepository).save(any());
    }

    @Test
    void createIssue_nonTeamMember_throwsUnauthorized() throws Exception {
        Long projectId = 1L;
        User owner = new User();
        owner.setId(1L);
        User stranger = new User();
        stranger.setId(99L);
        Project project = new Project();
        project.setId(projectId);
        project.setOwner(owner);
        project.setTeam(Collections.emptyList());

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());

        IssueRequest request = new IssueRequest();
        request.setTitle("T");
        request.setDescription("D");
        request.setPriority(ISSUE_PRIORITY.LOW.toString());

        assertThrows(UnauthorizedException.class, () -> issueService.createIssue(projectId, request, stranger));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void createIssue_teamMember_savesWithNullSprint() throws Exception {
        Long projectId = 1L;
        User member = new User();
        member.setId(5L);
        Project project = new Project();
        project.setId(projectId);
        project.setOwner(member);

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), eq(project)))
                .thenReturn(IssueResponse.builder().id(1L).build());

        IssueRequest request = new IssueRequest();
        request.setTitle("T");
        request.setDescription("D");
        request.setPriority(ISSUE_PRIORITY.MEDIUM.toString());
        request.setDueDate(LocalDate.now());

        issueService.createIssue(projectId, request, member);

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(captor.capture());
        assertNull(captor.getValue().getSprint());
        assertEquals(member, captor.getValue().getAssignedBy());
    }

    @Test
    void deleteIssue_inCompletedSprint_throwsBadRequest() throws Exception {
        Long issueId = 77L;
        Sprint sprint = new Sprint();
        sprint.setId(3L);
        sprint.setStatus(SPRINT_STATUS.COMPLETED);
        User owner = new User();
        owner.setId(1L);
        Project project = new Project();
        project.setOwner(owner);
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setSprint(sprint);
        issue.setProject(project);
        issue.setCreatedBy(owner);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        assertThrows(BadRequestException.class, () -> issueService.deleteIssue(issueId, owner.getId()));
        verify(issueRepository, never()).deleteById(any());
    }

    @Test
    void assignIssueSprint_memberCannotAssignToInactiveSprint() {
        Long issueId = 7L;
        Long memberId = 5L;
        Long ownerId = 1L;
        Long sprintId = 50L;

        User owner = new User();
        owner.setId(ownerId);
        User member = new User();
        member.setId(memberId);

        Project project = new Project();
        project.setId(3L);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.setOwner(owner);

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setCreatedBy(member);
        issue.setAssignee(null);

        IssueSprintAssignmentRequest req = new IssueSprintAssignmentRequest();
        req.setSprintId(sprintId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(sprintRepository.findByIdAndProject_Id(sprintId, project.getId())).thenReturn(Optional.of(sprint));
        when(userService.findByUserId(memberId)).thenReturn(member);
        when(projectAuthorizationService.canManageSprints(project, member)).thenReturn(false);
        when(projectAuthorizationService.canAdministerAllTasks(project.getId(), member)).thenReturn(false);
        when(projectAuthorizationService.mustRestrictSprintAssignmentToActive(project.getId(), member))
                .thenReturn(true);

        assertThrows(
                BadRequestException.class, () -> issueService.assignIssueSprint(issueId, req, memberId));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void updateIssue_assigneeOnly_throwsUnauthorized() throws Exception {
        Long issueId = 10L;
        Long assigneeId = 2L;
        Long creatorId = 50L;

        User assignee = new User();
        assignee.setId(assigneeId);
        User creator = new User();
        creator.setId(creatorId);
        User owner = new User();
        owner.setId(100L);
        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Old");
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(assignee);

        IssueRequest req = new IssueRequest();
        req.setTitle("New title");
        req.setDescription("Desc");
        req.setPriority(ISSUE_PRIORITY.LOW.toString());

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(assigneeId)).thenReturn(assignee);

        assertThrows(
                UnauthorizedException.class,
                () -> issueService.updateIssue(issueId, req, assigneeId));
        verify(issueRepository, never()).save(any());
    }
}
