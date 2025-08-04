package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.*;
import com.project.projectmanagementapplication.model.Invitation;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.security.JwtUtil;
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
    private final JwtUtil jwtUtil;

    @Autowired
    public EmailInviteController(InvitationService invitationService, ProjectService projectService, UserService userService, JwtUtil jwtUtil) {
        this.invitationService = invitationService;
        this.projectService = projectService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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

    // NEW PUBLIC ENDPOINTS (no authentication required)
    @GetMapping("/details/{token}")
    public ResponseEntity<Response<ProjectDetailsResponse>> getInvitationDetails(
            @PathVariable String token) {
        try {
            Response<ProjectDetailsResponse> response = invitationService.getInvitationDetails(token);
            return new ResponseEntity<>(response, response.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.<ProjectDetailsResponse>builder()
                            .message("Error retrieving invitation: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<Response<InvitationAcceptanceResponse>> acceptInvitationPublic(
            @PathVariable String token,
            @RequestBody InvitationAcceptRequest request) {
        try {
            Response<InvitationAcceptanceResponse> response =
                    invitationService.processInvitationAcceptance(token, request.getUserEmail());
            return new ResponseEntity<>(response, response.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.<InvitationAcceptanceResponse>builder()
                            .message("Error accepting invitation: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }

    // Endpoint to be called after user registration/login
    @PostMapping("/process-pending")
    public ResponseEntity<Response<Void>> processPendingInvitations() {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Response<Void> response = invitationService.acceptInvitationAfterRegistration(username);
            return new ResponseEntity<>(response, response.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.<Void>builder()
                            .message("Error processing pending invitations: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }
}

