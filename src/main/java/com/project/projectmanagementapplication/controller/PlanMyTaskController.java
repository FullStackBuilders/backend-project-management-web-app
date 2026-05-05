package com.project.projectmanagementapplication.controller;

import com.google.genai.Client;
import com.project.projectmanagementapplication.dto.PlanMyTaskRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.ai.MetricsInsightsResponse;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.service.GeminiPlanMyTaskService;
import com.project.projectmanagementapplication.service.ProjectService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnBean(Client.class)
public class PlanMyTaskController {

    private final GeminiPlanMyTaskService geminiPlanMyTaskService;
    private final IssueRepository issueRepository;
    private final UserService userService;
    private final ProjectService projectService;

    public PlanMyTaskController(
            GeminiPlanMyTaskService geminiPlanMyTaskService,
            IssueRepository issueRepository,
            UserService userService,
            ProjectService projectService) {
        this.geminiPlanMyTaskService = geminiPlanMyTaskService;
        this.issueRepository = issueRepository;
        this.userService = userService;
        this.projectService = projectService;
    }

    @PostMapping("/plan-my-tasks")
    public ResponseEntity<Response<MetricsInsightsResponse>> planMyTasks(
            @RequestBody PlanMyTaskRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Project project = projectService.getProjectById(request.getProjectId()).getData();

        List<Issue> activeTasks = issueRepository.findByProject_IdAndAssignee_IdAndStatusNot(
                project.getId(), user.getId(), ISSUE_STATUS.DONE);

        if (activeTasks.isEmpty()) {
            throw new BadRequestException(
                    "You have no active tasks assigned to you in this project. Nothing to plan!");
        }

        MetricsInsightsResponse plan = geminiPlanMyTaskService.generatePlan(
                activeTasks, project.getName(), LocalDate.now());

        return ResponseEntity.ok(
                Response.<MetricsInsightsResponse>builder()
                        .message("Task plan generated successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now().toString())
                        .data(plan)
                        .build());
    }
}
