package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.SprintCreateRequest;
import com.project.projectmanagementapplication.dto.SprintResponse;
import com.project.projectmanagementapplication.dto.SprintStartRequest;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.mapper.SprintMapper;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceImplTest {

    @InjectMocks
    private SprintServiceImpl sprintService;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private ProjectService projectService;

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
                .when(projectAuthorizationService.usesMemberSprintListFilter(any(Long.class), any(User.class)))
                .thenReturn(false);
    }

    private static Project scrumProject(long projectId, long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Project p = new Project();
        p.setId(projectId);
        p.setFramework(PROJECT_FRAMEWORK.SCRUM);
        p.setOwner(owner);
        return p;
    }

    @Test
    void createSprint_owner_succeedsWithInactiveStatus() {
        long projectId = 1L;
        long ownerId = 10L;
        Project project = scrumProject(projectId, ownerId);
        User owner = project.getOwner();

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("Sprint 1");
        req.setGoal("Goal");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .status(s.getStatus())
                    .build();
        });

        var response = sprintService.createSprint(projectId, req, owner);

        assertNotNull(response.getData());
        assertEquals(SPRINT_STATUS.INACTIVE, response.getData().getStatus());

        ArgumentCaptor<Sprint> cap = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).save(cap.capture());
        assertEquals("Sprint 1", cap.getValue().getName());
        assertEquals(SPRINT_STATUS.INACTIVE, cap.getValue().getStatus());
    }

    @Test
    void createSprint_nonOwner_throwsUnauthorized() {
        long projectId = 1L;
        Project project = scrumProject(projectId, 1L);
        User notOwner = new User();
        notOwner.setId(2L);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("S");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());

        assertThrows(UnauthorizedException.class, () -> sprintService.createSprint(projectId, req, notOwner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprint_kanbanProject_throwsBadRequest() {
        long projectId = 1L;
        User owner = new User();
        owner.setId(10L);
        Project project = new Project();
        project.setId(projectId);
        project.setFramework(PROJECT_FRAMEWORK.KANBAN);
        project.setOwner(owner);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("S");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(1));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());

        assertThrows(BadRequestException.class, () -> sprintService.createSprint(projectId, req, owner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprint_inactiveOwner_succeeds() {
        long projectId = 1L;
        long ownerId = 10L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, ownerId);
        User owner = project.getOwner();

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);
        sprint.setName("Old");
        sprint.setStartDate(LocalDate.of(2026, 4, 1));
        sprint.setEndDate(LocalDate.of(2026, 4, 14));

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("New name");
        req.setGoal(null);
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .status(s.getStatus())
                    .build();
        });

        var response = sprintService.updateSprint(projectId, sprintId, req, owner);

        assertEquals("New name", response.getData().getName());
        ArgumentCaptor<Sprint> cap = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).save(cap.capture());
        assertEquals("New name", cap.getValue().getName());
    }

    @Test
    void updateSprint_activeOwner_succeeds() {
        long projectId = 1L;
        long ownerId = 10L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, ownerId);
        User owner = project.getOwner();

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.ACTIVE);
        sprint.setProject(project);
        sprint.setName("S1");
        sprint.setStartDate(LocalDate.of(2026, 4, 1));
        sprint.setEndDate(LocalDate.of(2026, 4, 14));

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("S1 renamed");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder().id(s.getId()).name(s.getName()).status(s.getStatus()).build();
        });

        var response = sprintService.updateSprint(projectId, sprintId, req, owner);

        assertEquals("S1 renamed", response.getData().getName());
    }

    @Test
    void updateSprint_completed_throwsBadRequest() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.COMPLETED);
        sprint.setProject(project);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("X");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));

        assertThrows(BadRequestException.class, () -> sprintService.updateSprint(projectId, sprintId, req, owner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprint_nonOwner_throwsUnauthorized() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 1L);
        User notOwner = new User();
        notOwner.setId(2L);

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("X");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));

        assertThrows(UnauthorizedException.class, () -> sprintService.updateSprint(projectId, sprintId, req, notOwner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprint_notFound_throwsResourceNotFound() {
        long projectId = 1L;
        long sprintId = 99L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("X");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.updateSprint(projectId, sprintId, req, owner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprint_endBeforeStart_throwsBadRequest() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("X");
        req.setStartDate(LocalDate.of(2026, 4, 10));
        req.setEndDate(LocalDate.of(2026, 4, 1));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));

        assertThrows(BadRequestException.class, () -> sprintService.updateSprint(projectId, sprintId, req, owner));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void startSprint_owner_setsActive() {
        long projectId = 1L;
        long ownerId = 10L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, ownerId);
        User owner = project.getOwner();

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);
        sprint.setEndDate(LocalDate.now().plusDays(1));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(issueRepository.countBySprint_Id(sprintId)).thenReturn(1L);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder().id(s.getId()).status(s.getStatus()).build();
        });

        var response = sprintService.startSprint(projectId, sprintId, owner, null);

        assertEquals(SPRINT_STATUS.ACTIVE, response.getData().getStatus());
        ArgumentCaptor<Sprint> cap = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).save(cap.capture());
        assertEquals(SPRINT_STATUS.ACTIVE, cap.getValue().getStatus());
    }

    @Test
    void startSprint_noTasks_throwsBadRequest() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();
        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setEndDate(LocalDate.now().plusDays(1));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(issueRepository.countBySprint_Id(sprintId)).thenReturn(0L);

        assertThrows(
                BadRequestException.class,
                () -> sprintService.startSprint(projectId, sprintId, owner, null));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void startSprint_pastEndWithoutBody_throwsBadRequest() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();
        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setEndDate(LocalDate.now().minusDays(1));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(issueRepository.countBySprint_Id(sprintId)).thenReturn(1L);

        assertThrows(
                BadRequestException.class,
                () -> sprintService.startSprint(projectId, sprintId, owner, null));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void startSprint_withBody_updatesDatesAndActivates() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();
        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setEndDate(LocalDate.now().minusDays(5));

        SprintStartRequest req = new SprintStartRequest();
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(6));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(issueRepository.countBySprint_Id(sprintId)).thenReturn(2L);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder().id(s.getId()).status(s.getStatus()).build();
        });

        var response = sprintService.startSprint(projectId, sprintId, owner, req);

        assertEquals(SPRINT_STATUS.ACTIVE, response.getData().getStatus());
        assertEquals(req.getStartDate(), sprint.getStartDate());
        assertEquals(req.getEndDate(), sprint.getEndDate());
    }

    @Test
    void completeSprint_movesNonDoneToBacklog() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();
        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.ACTIVE);
        sprint.setProject(project);

        Issue done = new Issue();
        done.setStatus(ISSUE_STATUS.DONE);
        done.setSprint(sprint);
        Issue todo = new Issue();
        todo.setStatus(ISSUE_STATUS.TO_DO);
        todo.setSprint(sprint);

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId))
                .thenReturn(Optional.of(sprint))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findBySprint_Id(sprintId)).thenReturn(List.of(done, todo));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder().id(s.getId()).status(s.getStatus()).build();
        });

        var response = sprintService.completeSprint(projectId, sprintId, owner);

        assertEquals(SPRINT_STATUS.COMPLETED, response.getData().getStatus());
        assertNull(todo.getSprint());
        assertNotNull(done.getSprint());
        verify(issueRepository).save(todo);
        verify(issueRepository, never()).save(done);
    }

    @Test
    void completeSprint_alreadyCompleted_isIdempotent() {
        long projectId = 1L;
        long sprintId = 50L;
        Project project = scrumProject(projectId, 10L);
        User owner = project.getOwner();
        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setStatus(SPRINT_STATUS.COMPLETED);

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(sprintRepository.findByIdAndProject_Id(sprintId, projectId)).thenReturn(Optional.of(sprint));
        when(sprintMapper.toResponse(any(Sprint.class))).thenAnswer(inv -> {
            Sprint s = inv.getArgument(0);
            return SprintResponse.builder().id(s.getId()).status(s.getStatus()).build();
        });

        var response = sprintService.completeSprint(projectId, sprintId, owner);

        assertEquals(SPRINT_STATUS.COMPLETED, response.getData().getStatus());
        verify(sprintRepository, never()).save(any());
        verify(issueRepository, never()).findBySprint_Id(any());
    }

    @Test
    void listSprints_memberRole_excludesInactiveSprints() {
        long projectId = 1L;
        User member = new User();
        member.setId(2L);
        Project project = scrumProject(projectId, 10L);
        project.getTeam().add(member);

        Sprint inactive = new Sprint();
        inactive.setId(1L);
        inactive.setStatus(SPRINT_STATUS.INACTIVE);
        Sprint active = new Sprint();
        active.setId(2L);
        active.setStatus(SPRINT_STATUS.ACTIVE);
        Sprint completed = new Sprint();
        completed.setId(3L);
        completed.setStatus(SPRINT_STATUS.COMPLETED);

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(projectAuthorizationService.usesMemberSprintListFilter(projectId, member)).thenReturn(true);
        when(sprintRepository.findByProject_IdOrderByStartDateDesc(projectId))
                .thenReturn(List.of(inactive, active, completed));
        when(sprintMapper.toResponse(active))
                .thenReturn(
                        SprintResponse.builder().id(2L).name("A").status(SPRINT_STATUS.ACTIVE).build());
        when(sprintMapper.toResponse(completed))
                .thenReturn(
                        SprintResponse.builder()
                                .id(3L)
                                .name("C")
                                .status(SPRINT_STATUS.COMPLETED)
                                .build());

        var response = sprintService.listSprints(projectId, member);

        assertEquals(2, response.getData().size());
        verify(sprintMapper, never()).toResponse(eq(inactive));
    }

    @Test
    void createSprint_scrumMaster_whenAuthorized_succeeds() {
        long projectId = 1L;
        Project project = scrumProject(projectId, 10L);
        User scrumMaster = new User();
        scrumMaster.setId(3L);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("Sprint 1");
        req.setStartDate(LocalDate.of(2026, 4, 1));
        req.setEndDate(LocalDate.of(2026, 4, 14));

        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(projectAuthorizationService.canManageSprints(project, scrumMaster)).thenReturn(true);
        when(sprintRepository.save(any(Sprint.class)))
                .thenAnswer(
                        inv -> {
                            Sprint s = inv.getArgument(0);
                            s.setId(100L);
                            return s;
                        });
        when(sprintMapper.toResponse(any(Sprint.class)))
                .thenAnswer(
                        inv -> {
                            Sprint s = inv.getArgument(0);
                            return SprintResponse.builder()
                                    .id(s.getId())
                                    .name(s.getName())
                                    .status(s.getStatus())
                                    .build();
                        });

        var response = sprintService.createSprint(projectId, req, scrumMaster);

        assertNotNull(response.getData());
        assertEquals(SPRINT_STATUS.INACTIVE, response.getData().getStatus());
    }
}
