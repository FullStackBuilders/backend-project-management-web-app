package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.EmailInvitationResponse;
import com.project.projectmanagementapplication.dto.EmailInviteRequest;
import com.project.projectmanagementapplication.dto.ProjectDetailsResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Invitation;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.InvitationService;
import com.project.projectmanagementapplication.service.ProjectService;
import com.project.projectmanagementapplication.service.UserService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/invitations")
public class EmailInviteController {

    private final InvitationService invitationService;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public EmailInviteController(InvitationService invitationService, ProjectService projectService, UserService userService) {
        this.invitationService = invitationService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @PostMapping("/send")
    public ResponseEntity<Response<EmailInvitationResponse>> sendInvitation(
            @RequestBody EmailInviteRequest request) throws MessagingException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Response<EmailInvitationResponse> response = invitationService.sendInvitation(
                request.getEmail(),
                user.getId(),
                request.getProjectId(),
                request.isForceResend()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<Response<Void>> acceptInvitation(@RequestParam String token) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userService.findByUsername(username);

            Response<Invitation> invitationResponse = invitationService.acceptInvitation(token, user.getId());
            Invitation invitation = invitationResponse.getData();

            // Add user to project
            projectService.addUserToProject(user.getId(), invitation.getProjectId());

            // Clean up invitation token
            invitationService.deleteToken(token);

            return ResponseEntity.ok(Response.<Void>builder()
                    .message("Successfully joined the project!")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.<Void>builder()
                            .message("Failed to accept invitation: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }

    @GetMapping("/details")
    public ResponseEntity<Response<ProjectDetailsResponse>> getInvitationDetails(@RequestParam String token) {
        try {
            Response<Invitation> response = invitationService.acceptInvitation(token, null);
            Invitation invitation = response.getData();

            // Get project details for the invitation
            Response<Project> projectResponse = projectService.getProjectById(invitation.getProjectId());
            Project project = projectResponse.getData();

            ProjectDetailsResponse details = ProjectDetailsResponse.builder()
                    .projectName(project.getName())
                    .projectDescription(project.getDescription())
                    .projectCategory(project.getCategory())
                    .ownerName(project.getOwner().getFirstName())
                    .teamSize(project.getTeam().size())
                    .token(token)
                    .build();

            return ResponseEntity.ok(Response.<ProjectDetailsResponse>builder()
                    .message("Invitation details retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .data(details)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Response.<ProjectDetailsResponse>builder()
                            .message("Invitation not found or expired")
                            .status(HttpStatus.NOT_FOUND)
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }
}

