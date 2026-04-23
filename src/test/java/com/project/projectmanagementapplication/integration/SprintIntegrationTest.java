package com.project.projectmanagementapplication.integration;

import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.integration.base.IntegrationTestBase;
import com.project.projectmanagementapplication.model.Project;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintIntegrationTest extends IntegrationTestBase {

    @Test
    void createSprint_deniesMemberRole() throws Exception {
        var owner = createUser("sprint.create.owner@example.com");
        var member = createUser("sprint.create.member@example.com");
        Project project = createProjectViaService(owner, "Sprint Create Project", PROJECT_FRAMEWORK.SCRUM);
        addMemberWithRole(project, member, PROJECT_MEMBER_ROLE.MEMBER);

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(14);
        String body =
                """
                {
                  "name":"Sprint 1",
                  "goal":"Deliver first milestone",
                  "startDate":"%s",
                  "endDate":"%s"
                }
                """
                        .formatted(start, end);

        mockMvc.perform(
                        post("/api/projects/{projectId}/sprints", project.getId())
                                .header("Authorization", bearerTokenFor(member))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startSprint_requiresAtLeastOneTask() throws Exception {
        var owner = createUser("sprint.start.owner@example.com");
        Project project = createProjectViaService(owner, "Sprint Start Project", PROJECT_FRAMEWORK.SCRUM);
        var sprint = createSprint(project, "Ready Sprint", SPRINT_STATUS.INACTIVE);

        mockMvc.perform(
                        post("/api/projects/{projectId}/sprints/{sprintId}/start", project.getId(), sprint.getId())
                                .header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeSprint_movesUnfinishedIssuesToBacklog() throws Exception {
        var owner = createUser("sprint.complete.owner@example.com");
        Project project = createProjectViaService(owner, "Sprint Complete Project", PROJECT_FRAMEWORK.SCRUM);
        var activeSprint = createSprint(project, "Active Sprint", SPRINT_STATUS.ACTIVE);
        var doneIssue = createIssue(project, owner, owner, ISSUE_STATUS.DONE, activeSprint);
        var todoIssue = createIssue(project, owner, owner, ISSUE_STATUS.TO_DO, activeSprint);

        mockMvc.perform(
                        post("/api/projects/{projectId}/sprints/{sprintId}/complete", project.getId(), activeSprint.getId())
                                .header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sprint completed successfully"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        var refreshedSprint = sprintRepository.findById(activeSprint.getId()).orElseThrow();
        assertThat(refreshedSprint.getStatus()).isEqualTo(SPRINT_STATUS.COMPLETED);

        var refreshedDoneIssue = issueRepository.findById(doneIssue.getId()).orElseThrow();
        var refreshedTodoIssue = issueRepository.findById(todoIssue.getId()).orElseThrow();
        assertThat(refreshedDoneIssue.getSprint()).isNotNull();
        assertThat(refreshedDoneIssue.getSprint().getId()).isEqualTo(activeSprint.getId());
        assertThat(refreshedTodoIssue.getSprint()).isNull();
    }
}
