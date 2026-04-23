package com.project.projectmanagementapplication.integration;

import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.integration.base.IntegrationTestBase;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectIntegrationTest extends IntegrationTestBase {

    @Test
    void createProject_persistsOwnerMembershipAndChat() throws Exception {
        var owner = createUser("owner.project@example.com");

        String body =
                """
                {
                  "name":"Platform Revamp",
                  "description":"Project for platform migration",
                  "category":"Engineering",
                  "tags":["backend","release"],
                  "framework":"SCRUM"
                }
                """;

        mockMvc.perform(
                        post("/api/projects")
                                .header("Authorization", bearerTokenFor(owner))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Platform Revamp"))
                .andExpect(jsonPath("$.framework").value("SCRUM"));

        assertThat(projectRepository.findAll()).hasSize(1);
        Project savedProject = projectRepository.findAll().get(0);
        assertThat(savedProject.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(projectRepository.findByTeamContainingOrOwner(owner, owner))
                .extracting(Project::getId)
                .contains(savedProject.getId());

        var ownerMembership =
                projectMembershipRepository.findByProject_IdAndUser_Id(savedProject.getId(), owner.getId());
        assertThat(ownerMembership).isPresent();
        assertThat(ownerMembership.get().getRole()).isEqualTo(PROJECT_MEMBER_ROLE.OWNER);

        assertThat(chatRepository.findAll()).hasSize(1);
        Chat chat = chatRepository.findAll().get(0);
        assertThat(chat.getProject().getId()).isEqualTo(savedProject.getId());
    }

    @Test
    void getProjects_returnsOnlyProjectsVisibleToCaller() throws Exception {
        var caller = createUser("caller.project@example.com");
        var outsider = createUser("outsider.project@example.com");

        createProjectViaService(caller, "Caller Project", PROJECT_FRAMEWORK.KANBAN);
        createProjectViaService(outsider, "Other Project", PROJECT_FRAMEWORK.KANBAN);

        mockMvc.perform(get("/api/projects").header("Authorization", bearerTokenFor(caller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Caller Project"));
    }

    @Test
    void updateProjectMemberRole_deniesMemberCaller() throws Exception {
        var owner = createUser("owner.role@example.com");
        var memberCaller = createUser("member.caller@example.com");
        var target = createUser("target.member@example.com");

        Project project = createProjectViaService(owner, "Role Project", PROJECT_FRAMEWORK.SCRUM);
        addMemberWithRole(project, memberCaller, PROJECT_MEMBER_ROLE.MEMBER);
        addMemberWithRole(project, target, PROJECT_MEMBER_ROLE.MEMBER);

        String body = "{\"role\":\"ADMIN\"}";

        mockMvc.perform(
                        patch("/api/projects/{projectId}/members/m/{memberUserId}/role", project.getId(), target.getId())
                                .header("Authorization", bearerTokenFor(memberCaller))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProjectMemberRole_allowsAdminCallerAndPersistsRole() throws Exception {
        var owner = createUser("owner.admin@example.com");
        var admin = createUser("admin.user@example.com");
        var target = createUser("target.user@example.com");

        Project project = createProjectViaService(owner, "Admin Role Project", PROJECT_FRAMEWORK.SCRUM);
        addMemberWithRole(project, admin, PROJECT_MEMBER_ROLE.ADMIN);
        addMemberWithRole(project, target, PROJECT_MEMBER_ROLE.MEMBER);

        String body = "{\"role\":\"SCRUM_MASTER\"}";

        mockMvc.perform(
                        patch("/api/projects/{projectId}/members/m/{memberUserId}/role", project.getId(), target.getId())
                                .header("Authorization", bearerTokenFor(admin))
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCRUM_MASTER"));

        var updated =
                projectMembershipRepository.findByProject_IdAndUser_Id(project.getId(), target.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getRole()).isEqualTo(PROJECT_MEMBER_ROLE.SCRUM_MASTER);
    }
}
