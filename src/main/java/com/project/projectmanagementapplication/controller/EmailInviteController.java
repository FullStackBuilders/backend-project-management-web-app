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

import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


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
    public ResponseEntity<Response<Void>> acceptInvitation(@RequestParam String token) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Response<Invitation> invitationResponse = invitationService.acceptInvitation(token, user.getId());
        Invitation invitation = invitationResponse.getData();

        projectService.addUserToProject(user.getId(), invitation.getProjectId());
        invitationService.deleteToken(token);

        return ResponseEntity.ok(Response.<Void>builder()
                .message("Successfully joined the project!")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    @GetMapping("/details/{token}")
    public ResponseEntity<Response<ProjectDetailsResponse>> getInvitationDetails(
            @PathVariable String token) throws Exception {
        Response<ProjectDetailsResponse> response = invitationService.getInvitationDetails(token);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<Response<InvitationAcceptanceResponse>> acceptInvitationPublic(
            @PathVariable String token,
            @RequestBody InvitationAcceptRequest request) throws Exception {
        Response<InvitationAcceptanceResponse> response =
                invitationService.processInvitationAcceptance(token, request.getUserEmail());
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/process-pending")
    public ResponseEntity<Response<Boolean>> processPendingInvitations() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Response<Boolean> response = invitationService.acceptInvitationAfterRegistration(username);
        return new ResponseEntity<>(response, response.getStatus());
    }
}

