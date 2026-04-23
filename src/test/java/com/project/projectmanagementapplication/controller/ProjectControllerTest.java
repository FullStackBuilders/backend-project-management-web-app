package com.project.projectmanagementapplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.BoardColumnLimitResponse;
import com.project.projectmanagementapplication.dto.ProjectMembershipMeResponse;
import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.service.BoardColumnLimitService;
import com.project.projectmanagementapplication.service.ChatService;
import com.project.projectmanagementapplication.service.InvitationService;
import com.project.projectmanagementapplication.service.ProjectMembershipService;
import com.project.projectmanagementapplication.service.ProjectService;
import com.project.projectmanagementapplication.service.UserService;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.filter.JwtFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProjectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private ProjectMembershipService projectMembershipService;

    @MockitoBean
    private BoardColumnLimitService boardColumnLimitService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("pm@example.com");
        when(userService.findByUsername("pm@example.com")).thenReturn(user);
    }

    private static <T> Response<T> response(HttpStatus status, T data) {
        return Response.<T>builder()
                .accessToken(null)
                .message("ok")
                .status(status)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now().toString())
                .data(data)
                .build();
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void getProjects_returnsListBody() throws Exception {
        Project p = new Project();
        p.setId(10L);
        p.setName("Alpha");
        p.setFramework(PROJECT_FRAMEWORK.KANBAN);
        when(projectService.getAllProjectForUser(eq(user), isNull(), isNull()))
                .thenReturn(response(HttpStatus.OK, List.of(p)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Alpha"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void getProjectById_propagatesServiceStatus() throws Exception {
        when(projectService.getProjectByIdForUser(eq(99L), eq(user)))
                .thenReturn(response(HttpStatus.NOT_FOUND, null));

        mockMvc.perform(get("/api/projects/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void createProject_returnsCreatedBody() throws Exception {
        Project created = new Project();
        created.setId(3L);
        created.setName("New board");
        created.setFramework(PROJECT_FRAMEWORK.SCRUM);
        ProjectRequest body = new ProjectRequest();
        body.setName("New board");
        body.setDescription("Desc");
        body.setFramework("SCRUM");
        when(projectService.createProject(any(ProjectRequest.class), eq(user)))
                .thenReturn(response(HttpStatus.CREATED, created));

        mockMvc.perform(
                        post("/api/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("New board"))
                .andExpect(jsonPath("$.framework").value("SCRUM"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void updateProject_returnsUpdatedProjectBody() throws Exception {
        Project updated = new Project();
        updated.setId(12L);
        updated.setName("Renamed");
        updated.setFramework(PROJECT_FRAMEWORK.KANBAN);
        ProjectRequest body = new ProjectRequest();
        body.setName("Renamed");
        body.setDescription("Updated desc");
        when(projectService.updateProject(any(ProjectRequest.class), eq(12L), eq(user)))
                .thenReturn(response(HttpStatus.OK, updated));

        mockMvc.perform(
                        patch("/api/projects/12")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void getMyProjectMembership_returnsRole() throws Exception {
        when(projectService.getProjectById(20L)).thenReturn(response(HttpStatus.OK, new Project()));
        when(projectMembershipService.getRole(20L, 7L)).thenReturn(PROJECT_MEMBER_ROLE.ADMIN);

        mockMvc.perform(get("/api/projects/20/membership/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void updateProjectMemberRole_returnsUpdatedRole() throws Exception {
        Project project = new Project();
        project.setId(22L);
        when(projectService.getProjectById(22L)).thenReturn(response(HttpStatus.OK, project));
        when(projectMembershipService.updateMemberRole(eq(project), eq(user), eq(9L), eq(PROJECT_MEMBER_ROLE.SCRUM_MASTER)))
                .thenReturn(PROJECT_MEMBER_ROLE.SCRUM_MASTER);

        mockMvc.perform(
                        patch("/api/projects/22/members/m/9/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"SCRUM_MASTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCRUM_MASTER"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void deleteProject_returnsWrappedSuccessResponse() throws Exception {
        when(projectService.deleteProject(33L, 7L)).thenReturn(response(HttpStatus.OK, null));

        mockMvc.perform(delete("/api/projects/33"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void getBoardColumnLimits_returnsList() throws Exception {
        BoardColumnLimitResponse todo =
                BoardColumnLimitResponse.builder()
                        .status("TO_DO")
                        .wipLimit(5)
                        .currentCount(2)
                        .exceeded(false)
                        .build();
        when(boardColumnLimitService.getColumnLimits(44L, user)).thenReturn(List.of(todo));

        mockMvc.perform(get("/api/projects/44/board-columns/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("TO_DO"))
                .andExpect(jsonPath("$[0].wipLimit").value(5))
                .andExpect(jsonPath("$[0].currentCount").value(2));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void updateBoardColumnLimit_returnsUpdatedLimit() throws Exception {
        BoardColumnLimitResponse updated =
                BoardColumnLimitResponse.builder()
                        .status("IN_PROGRESS")
                        .wipLimit(3)
                        .currentCount(1)
                        .exceeded(false)
                        .build();
        when(boardColumnLimitService.updateColumnLimit(eq(45L), eq("IN_PROGRESS"), any(), eq(user)))
                .thenReturn(updated);

        mockMvc.perform(
                        put("/api/projects/45/board-columns/IN_PROGRESS/limit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"wipLimit\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.wipLimit").value(3));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void clearBoardColumnLimit_returnsClearedLimit() throws Exception {
        BoardColumnLimitResponse cleared =
                BoardColumnLimitResponse.builder()
                        .status("DONE")
                        .wipLimit(null)
                        .currentCount(0)
                        .exceeded(false)
                        .build();
        when(boardColumnLimitService.clearColumnLimit(46L, "DONE", user)).thenReturn(cleared);

        mockMvc.perform(delete("/api/projects/46/board-columns/DONE/limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.currentCount").value(0));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void searchProjects_returnsMatchingProjects() throws Exception {
        Project p = new Project();
        p.setId(50L);
        p.setName("Alpha Search");
        p.setFramework(PROJECT_FRAMEWORK.KANBAN);
        when(projectService.searchProjects("alpha", user))
                .thenReturn(response(HttpStatus.OK, List.of(p)));

        mockMvc.perform(get("/api/projects/search").param("keyword", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].name").value("Alpha Search"));
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void getChatByProjectId_returnsChatBody() throws Exception {
        Chat chat = new Chat();
        chat.setId(60L);
        chat.setName("Project Chat");
        when(chatService.getChatByProjectId(60L)).thenReturn(response(HttpStatus.OK, chat));

        mockMvc.perform(get("/api/projects/60/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(60))
                .andExpect(jsonPath("$.name").value("Project Chat"));
    }
}
