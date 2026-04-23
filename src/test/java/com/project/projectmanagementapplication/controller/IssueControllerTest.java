package com.project.projectmanagementapplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.IssueCountsResponse;
import com.project.projectmanagementapplication.dto.IssueDetailResponse;
import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.IssueSprintAssignmentRequest;
import com.project.projectmanagementapplication.dto.IssueTimelineData;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.filter.JwtFilter;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.IssueService;
import com.project.projectmanagementapplication.service.UserService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IssueController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("issue.pm@example.com");
        when(userService.findByUsername("issue.pm@example.com")).thenReturn(user);
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

    private static IssueResponse issueResponse(long id, String title, String status) {
        return IssueResponse.builder()
                .id(id)
                .title(title)
                .status(status)
                .priority("MEDIUM")
                .dueDate(LocalDate.now().plusDays(3))
                .build();
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getIssueById_returnsIssueEntity() throws Exception {
        Issue issue = new Issue();
        issue.setId(11L);
        issue.setTitle("Issue entity");
        when(issueService.getIssueById(11L)).thenReturn(issue);

        mockMvc.perform(get("/api/issues/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.title").value("Issue entity"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getIssuesByProjectId_returnsWrappedList() throws Exception {
        when(issueService.getIssueByProjectId(20L))
                .thenReturn(response(HttpStatus.OK, List.of(issueResponse(1L, "One", "TO_DO"))));

        mockMvc.perform(get("/api/issues/project/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("One"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getBacklogIssues_returnsWrappedList() throws Exception {
        when(issueService.getBacklogIssues(21L, user))
                .thenReturn(response(HttpStatus.OK, List.of(issueResponse(2L, "Backlog", "TO_DO"))));

        mockMvc.perform(get("/api/issues/project/21/backlog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Backlog"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void createIssue_returnsCreatedWrappedBody() throws Exception {
        IssueRequest body = new IssueRequest();
        body.setTitle("New issue");
        body.setDescription("desc");
        body.setPriority("HIGH");
        body.setDueDate(LocalDate.now().plusDays(2));

        when(issueService.createIssue(eq(22L), any(IssueRequest.class), eq(user)))
                .thenReturn(response(HttpStatus.CREATED, issueResponse(3L, "New issue", "TO_DO")));

        mockMvc.perform(
                        post("/api/issues/22")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.title").value("New issue"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void deleteIssue_returnsWrappedResponse() throws Exception {
        when(issueService.deleteIssue(30L, 7L)).thenReturn(response(HttpStatus.OK, 7L));

        mockMvc.perform(delete("/api/issues/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void updateIssue_returnsWrappedResponse() throws Exception {
        IssueRequest body = new IssueRequest();
        body.setTitle("Updated");
        body.setDescription("Updated desc");
        body.setPriority("MEDIUM");
        body.setDueDate(LocalDate.now().plusDays(5));

        when(issueService.updateIssue(eq(31L), any(IssueRequest.class), eq(7L)))
                .thenReturn(response(HttpStatus.OK, issueResponse(31L, "Updated", "IN_PROGRESS")));

        mockMvc.perform(
                        put("/api/issues/31")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void assignIssueSprint_returnsWrappedResponse() throws Exception {
        IssueSprintAssignmentRequest body = new IssueSprintAssignmentRequest();
        body.setSprintId(88L);
        when(issueService.assignIssueSprint(eq(32L), any(IssueSprintAssignmentRequest.class), eq(7L)))
                .thenReturn(response(HttpStatus.OK, issueResponse(32L, "Sprint assigned", "TO_DO")));

        mockMvc.perform(
                        patch("/api/issues/32/sprint")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(32))
                .andExpect(jsonPath("$.data.title").value("Sprint assigned"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void addUserToIssue_returnsWrappedResponse() throws Exception {
        when(issueService.addUserToIssue(33L, 8L, 7L))
                .thenReturn(response(HttpStatus.OK, issueResponse(33L, "Assigned", "IN_PROGRESS")));

        mockMvc.perform(put("/api/issues/33/assignee/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(33))
                .andExpect(jsonPath("$.data.title").value("Assigned"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void removeAssigneeFromIssue_returnsWrappedResponse() throws Exception {
        when(issueService.removeAssigneeFromIssue(34L, 7L))
                .thenReturn(response(HttpStatus.OK, issueResponse(34L, "Unassigned", "TO_DO")));

        mockMvc.perform(delete("/api/issues/34/assignee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(34))
                .andExpect(jsonPath("$.data.title").value("Unassigned"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void updateIssueStatus_returnsWrappedResponse() throws Exception {
        when(issueService.updateIssueStatus(35L, "DONE", 7L))
                .thenReturn(response(HttpStatus.OK, issueResponse(35L, "Done task", "DONE")));

        mockMvc.perform(put("/api/issues/35/status/DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(35))
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getIssueDetail_returnsWrappedDetail() throws Exception {
        IssueDetailResponse detail =
                IssueDetailResponse.builder()
                        .id(36L)
                        .title("Detail issue")
                        .projectId(99L)
                        .build();
        when(issueService.getIssueDetail(36L)).thenReturn(response(HttpStatus.OK, detail));

        mockMvc.perform(get("/api/issues/36/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(36))
                .andExpect(jsonPath("$.data.title").value("Detail issue"));
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getIssueTimeline_withExplicitLimit_wiresLimitAndReturnsPayload() throws Exception {
        IssueTimelineData timeline = IssueTimelineData.builder().items(List.of()).limit(50).build();
        when(issueService.getIssueTimeline(37L, 50)).thenReturn(response(HttpStatus.OK, timeline));

        mockMvc.perform(get("/api/issues/37/timeline").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.limit").value(50));

        verify(issueService).getIssueTimeline(37L, 50);
    }

    @Test
    @WithMockUser(username = "issue.pm@example.com")
    void getIssueCountsForCurrentUser_returnsWrappedSummary() throws Exception {
        IssueCountsResponse counts =
                IssueCountsResponse.builder()
                        .assignedTasks(5L)
                        .overdueTasks(1L)
                        .dueTodayTasks(2L)
                        .highPriorityTasks(3L)
                        .completedTasks(4L)
                        .build();
        when(issueService.getIssueCountsForUser(user)).thenReturn(response(HttpStatus.OK, counts));

        mockMvc.perform(get("/api/issues/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.assignedTasks").value(5))
                .andExpect(jsonPath("$.data.overdueTasks").value(1))
                .andExpect(jsonPath("$.data.completedTasks").value(4));
    }
}
