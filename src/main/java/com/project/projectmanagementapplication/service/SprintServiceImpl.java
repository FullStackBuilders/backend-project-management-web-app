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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
public class SprintServiceImpl implements SprintService {
    private static final Comparator<Sprint> SPRINT_LIST_ORDER = (a, b) -> {
        int byStart = compareNullableDesc(a.getStartDate(), b.getStartDate());
        if (byStart != 0) return byStart;
        int byCreated = compareNullableDesc(a.getCreatedAt(), b.getCreatedAt());
        if (byCreated != 0) return byCreated;
        return compareNullableDesc(a.getId(), b.getId());
    };

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final SprintMapper sprintMapper;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;

    public SprintServiceImpl(
            SprintRepository sprintRepository,
            IssueRepository issueRepository,
            SprintMapper sprintMapper,
            ProjectService projectService,
            ProjectAuthorizationService projectAuthorizationService) {
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.sprintMapper = sprintMapper;
        this.projectService = projectService;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<SprintResponse>> listSprints(Long projectId, User caller) {
        Project project = loadProject(projectId);
        assertScrumProject(project);
        assertProjectMember(project, caller);

        Stream<Sprint> stream = sprintRepository.findByProject_Id(projectId)
                .stream()
                .sorted(SPRINT_LIST_ORDER);
        if (projectAuthorizationService.usesMemberSprintListFilter(projectId, caller)) {
            stream = stream.filter(
                    s -> s.getStatus() == SPRINT_STATUS.ACTIVE || s.getStatus() == SPRINT_STATUS.COMPLETED);
        }
        List<SprintResponse> list = stream.map(sprintMapper::toResponse).toList();

        return Response.<List<SprintResponse>>builder()
                .data(list)
                .message(list.isEmpty() ? "No sprints found for this project" : "Sprints retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<SprintResponse> createSprint(Long projectId, SprintCreateRequest request, User owner) {
        Project project = loadProject(projectId);
        assertScrumProject(project);
        assertCanManageSprints(project, owner);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Sprint name is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        Sprint sprint = new Sprint();
        sprint.setName(request.getName().trim());
        sprint.setGoal(request.getGoal() != null ? request.getGoal().trim() : null);
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setStatus(SPRINT_STATUS.INACTIVE);
        sprint.setProject(project);

        Sprint saved = sprintRepository.save(sprint);
        SprintResponse dto = sprintMapper.toResponse(saved);

        return Response.<SprintResponse>builder()
                .data(dto)
                .message("Sprint created successfully")
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<SprintResponse> updateSprint(
            Long projectId, Long sprintId, SprintCreateRequest request, User owner) {
        Project project = loadProject(projectId);
        assertScrumProject(project);
        assertCanManageSprints(project, owner);

        Sprint sprint = sprintRepository
                .findByIdAndProject_Id(sprintId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found for this project"));

        if (sprint.getStatus() == SPRINT_STATUS.COMPLETED) {
            throw new BadRequestException("Cannot edit a completed sprint");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Sprint name is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        sprint.setName(request.getName().trim());
        sprint.setGoal(request.getGoal() != null ? request.getGoal().trim() : null);
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        Sprint saved = sprintRepository.save(sprint);
        SprintResponse dto = sprintMapper.toResponse(saved);

        return Response.<SprintResponse>builder()
                .data(dto)
                .message("Sprint updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<SprintResponse> startSprint(
            Long projectId, Long sprintId, User owner, SprintStartRequest request) {
        Project project = loadProject(projectId);
        assertScrumProject(project);
        assertCanManageSprints(project, owner);

        Sprint sprint = sprintRepository
                .findByIdAndProject_Id(sprintId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found for this project"));

        if (sprint.getStatus() == SPRINT_STATUS.COMPLETED) {
            throw new BadRequestException("Cannot start a completed sprint");
        }
        if (sprint.getStatus() != SPRINT_STATUS.INACTIVE) {
            throw new BadRequestException("Sprint is already active");
        }

        if (issueRepository.countBySprint_Id(sprintId) < 1) {
            throw new BadRequestException("Cannot start a sprint with no tasks");
        }

        LocalDate today = LocalDate.now();
        boolean hasStart = request != null && request.getStartDate() != null;
        boolean hasEnd = request != null && request.getEndDate() != null;
        if (hasStart != hasEnd) {
            throw new BadRequestException("startDate and endDate must both be provided when updating dates");
        }
        boolean bodyPresent = hasStart && hasEnd;
        if (bodyPresent) {
            applyStartDatesFromRequest(sprint, request, today);
        } else {
            if (sprint.getEndDate() == null) {
                throw new BadRequestException("Sprint end date is required to start");
            }
            if (sprint.getEndDate().isBefore(today)) {
                throw new BadRequestException(
                        "Sprint end date is in the past; provide startDate and endDate to update before starting");
            }
        }

        sprint.setStatus(SPRINT_STATUS.ACTIVE);
        Sprint saved = sprintRepository.save(sprint);

        return Response.<SprintResponse>builder()
                .data(sprintMapper.toResponse(saved))
                .message("Sprint started successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<SprintResponse> completeSprint(Long projectId, Long sprintId, User owner) {
        Project project = loadProject(projectId);
        assertScrumProject(project);
        assertCanManageSprints(project, owner);

        Sprint sprint = sprintRepository
                .findByIdAndProject_Id(sprintId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found for this project"));

        if (sprint.getStatus() == SPRINT_STATUS.COMPLETED) {
            return Response.<SprintResponse>builder()
                    .data(sprintMapper.toResponse(sprint))
                    .message("Sprint was already completed")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }
        if (sprint.getStatus() != SPRINT_STATUS.ACTIVE) {
            throw new BadRequestException("Only an active sprint can be completed");
        }

        sprint.setStatus(SPRINT_STATUS.COMPLETED);
        sprintRepository.save(sprint);

        List<Issue> inSprint = issueRepository.findBySprint_Id(sprintId);
        for (Issue issue : inSprint) {
            if (issue.getStatus() != ISSUE_STATUS.DONE) {
                issue.setSprint(null);
                issueRepository.save(issue);
            }
        }

        Sprint refreshed = sprintRepository
                .findByIdAndProject_Id(sprintId, projectId)
                .orElse(sprint);

        return Response.<SprintResponse>builder()
                .data(sprintMapper.toResponse(refreshed))
                .message("Sprint completed successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    private void applyStartDatesFromRequest(Sprint sprint, SprintStartRequest request, LocalDate today) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (start.isBefore(today)) {
            throw new BadRequestException("Start date must be today or in the future");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("End date must be on or after start date");
        }
        sprint.setStartDate(start);
        sprint.setEndDate(end);
    }

    private Project loadProject(Long projectId) {
        try {
            return projectService.getProjectById(projectId).getData();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Project not found with ID: " + projectId);
        }
    }

    private void assertScrumProject(Project project) {
        if (project.getFramework() != PROJECT_FRAMEWORK.SCRUM) {
            throw new BadRequestException("Sprints are only available for Scrum projects");
        }
    }

    private void assertCanManageSprints(Project project, User user) {
        if (!projectAuthorizationService.canManageSprints(project, user)) {
            throw new UnauthorizedException(
                    "Only the project owner, admin, or Scrum Master can manage sprints for this project");
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

    private static <T extends Comparable<? super T>> int compareNullableDesc(T left, T right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        return right.compareTo(left);
    }
}
