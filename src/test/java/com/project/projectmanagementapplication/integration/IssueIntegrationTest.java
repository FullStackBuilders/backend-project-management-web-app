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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IssueIntegrationTest extends IntegrationTestBase {

    @Test
    void createIssue_forProjectMember_persistsIssueAndActivity() throws Exception {
        var owner = createUser("issue.owner@example.com");
        var member = createUser("issue.member@example.com");
        Project project = createProjectViaService(owner, "Issue Create Project", PROJECT_FRAMEWORK.KANBAN);
        addMemberWithRole(project, member, PROJECT_MEMBER_ROLE.MEMBER);

        String body =
                """
                {
                  "title":"Setup CI checks",
                  "description":"Add pipeline checks for backend",
                  "priority":"HIGH",
                  "dueDate":"2030-01-15"
                }
                """;

        mockMvc.perform(
                        post("/api/issues/{projectId}", project.getId())
                                .header("Authorization", bearerTokenFor(member))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Issue created successfully"))
                .andExpect(jsonPath("$.data.title").value("Setup CI checks"))
                .andExpect(jsonPath("$.data.status").value("TO_DO"));

        assertThat(issueRepository.findAll()).hasSize(1);
        var created = issueRepository.findAll().get(0);
        assertThat(created.getProject().getId()).isEqualTo(project.getId());
        assertThat(created.getCreatedBy().getId()).isEqualTo(member.getId());
        assertThat(issueActivityRepository.findAll()).hasSize(1);
    }

    @Test
    void updateIssueStatus_forAssignee_persistsStatusTransitionAndActivity() throws Exception {
        var owner = createUser("status.owner@example.com");
        var assignee = createUser("status.assignee@example.com");
        Project project = createProjectViaService(owner, "Issue Status Project", PROJECT_FRAMEWORK.KANBAN);
        addMemberWithRole(project, assignee, PROJECT_MEMBER_ROLE.MEMBER);

        var issue = createIssue(project, owner, assignee, ISSUE_STATUS.TO_DO, null);

        mockMvc.perform(
                        put("/api/issues/{issueId}/status/{status}", issue.getId(), "IN_PROGRESS")
                                .header("Authorization", bearerTokenFor(assignee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Issue status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        var updated = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ISSUE_STATUS.IN_PROGRESS);
        assertThat(updated.getTaskStartedAt()).isNotNull();
        assertThat(issueActivityRepository.findAll()).hasSize(1);
    }

    @Test
    void assignIssueSprint_forMember_toInactiveSprint_returnsBadRequest() throws Exception {
        var owner = createUser("sprint.owner@example.com");
        var member = createUser("sprint.member@example.com");
        Project project = createProjectViaService(owner, "Sprint Assign Project", PROJECT_FRAMEWORK.SCRUM);
        addMemberWithRole(project, member, PROJECT_MEMBER_ROLE.MEMBER);
        var inactiveSprint = createSprint(project, "Planned Sprint", SPRINT_STATUS.INACTIVE);
        var issue = createIssue(project, member, member, ISSUE_STATUS.TO_DO, null);

        String body = "{\"sprintId\":" + inactiveSprint.getId() + "}";

        mockMvc.perform(
                        patch("/api/issues/{issueId}/sprint", issue.getId())
                                .header("Authorization", bearerTokenFor(member))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        var unchanged = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(unchanged.getSprint()).isNull();
    }
}
