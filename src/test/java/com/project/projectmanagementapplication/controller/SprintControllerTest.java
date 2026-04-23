package com.project.projectmanagementapplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.SprintCreateRequest;
import com.project.projectmanagementapplication.dto.SprintResponse;
import com.project.projectmanagementapplication.dto.SprintStartRequest;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.filter.JwtFilter;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.SprintService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SprintController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SprintService sprintService;

    @MockitoBean
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(17L);
        user.setEmail("sprint.pm@example.com");
        when(userService.findByUsername("sprint.pm@example.com")).thenReturn(user);
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

    private static SprintResponse sprintResponse(long id, String name, SPRINT_STATUS status) {
        return SprintResponse.builder()
                .id(id)
                .name(name)
                .goal("Goal")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .status(status)
                .projectId(100L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "sprint.pm@example.com")
    void listSprints_returnsWrappedList() throws Exception {
        when(sprintService.listSprints(100L, user))
                .thenReturn(response(HttpStatus.OK, List.of(sprintResponse(1L, "Sprint 1", SPRINT_STATUS.ACTIVE))));

        mockMvc.perform(get("/api/projects/100/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Sprint 1"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "sprint.pm@example.com")
    void createSprint_returnsCreatedWrappedBody() throws Exception {
        SprintCreateRequest body = new SprintCreateRequest();
        body.setName("Sprint A");
        body.setGoal("Deliver A");
        body.setStartDate(LocalDate.now().plusDays(1));
        body.setEndDate(LocalDate.now().plusDays(15));
        when(sprintService.createSprint(eq(100L), any(SprintCreateRequest.class), eq(user)))
                .thenReturn(response(HttpStatus.CREATED, sprintResponse(2L, "Sprint A", SPRINT_STATUS.INACTIVE)));

        mockMvc.perform(
                        post("/api/projects/100/sprints")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("Sprint A"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @WithMockUser(username = "sprint.pm@example.com")
    void updateSprint_returnsUpdatedWrappedBody() throws Exception {
        SprintCreateRequest body = new SprintCreateRequest();
        body.setName("Sprint A Updated");
        body.setGoal("Deliver updated");
        body.setStartDate(LocalDate.now().plusDays(2));
        body.setEndDate(LocalDate.now().plusDays(16));
        when(sprintService.updateSprint(eq(100L), eq(200L), any(SprintCreateRequest.class), eq(user)))
                .thenReturn(response(HttpStatus.OK, sprintResponse(200L, "Sprint A Updated", SPRINT_STATUS.INACTIVE)));

        mockMvc.perform(
                        put("/api/projects/100/sprints/200")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.name").value("Sprint A Updated"));
    }

    @Test
    @WithMockUser(username = "sprint.pm@example.com")
    void startSprint_withBody_returnsStartedWrappedBody() throws Exception {
        SprintStartRequest body = new SprintStartRequest();
        body.setStartDate(LocalDate.now().plusDays(1));
        body.setEndDate(LocalDate.now().plusDays(14));
        when(sprintService.startSprint(eq(100L), eq(201L), eq(user), any(SprintStartRequest.class)))
                .thenReturn(response(HttpStatus.OK, sprintResponse(201L, "Sprint Start", SPRINT_STATUS.ACTIVE)));

        mockMvc.perform(
                        post("/api/projects/100/sprints/201/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(201))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "sprint.pm@example.com")
    void completeSprint_returnsCompletedWrappedBody() throws Exception {
        when(sprintService.completeSprint(100L, 202L, user))
                .thenReturn(response(HttpStatus.OK, sprintResponse(202L, "Sprint Done", SPRINT_STATUS.COMPLETED)));

        mockMvc.perform(post("/api/projects/100/sprints/202/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(202))
                .andExpect(jsonPath("$.data.name").value("Sprint Done"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
