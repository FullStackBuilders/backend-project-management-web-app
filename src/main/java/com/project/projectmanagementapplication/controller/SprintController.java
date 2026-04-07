package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.SprintCreateRequest;
import com.project.projectmanagementapplication.dto.SprintResponse;
import com.project.projectmanagementapplication.dto.SprintStartRequest;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.SprintService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintController {

    private final SprintService sprintService;
    private final UserService userService;

    public SprintController(SprintService sprintService, UserService userService) {
        this.sprintService = sprintService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Response<List<SprintResponse>>> listSprints(@PathVariable Long projectId) {
        User caller = currentUser();
        Response<List<SprintResponse>> response = sprintService.listSprints(projectId, caller);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping
    public ResponseEntity<Response<SprintResponse>> createSprint(
            @PathVariable Long projectId,
            @RequestBody SprintCreateRequest request) {
        User owner = currentUser();
        Response<SprintResponse> response = sprintService.createSprint(projectId, request, owner);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{sprintId}/start")
    public ResponseEntity<Response<SprintResponse>> startSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestBody(required = false) SprintStartRequest request) {
        User owner = currentUser();
        Response<SprintResponse> response = sprintService.startSprint(projectId, sprintId, owner, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<Response<SprintResponse>> completeSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId) {
        User owner = currentUser();
        Response<SprintResponse> response = sprintService.completeSprint(projectId, sprintId, owner);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.findByUsername(username);
    }
}
