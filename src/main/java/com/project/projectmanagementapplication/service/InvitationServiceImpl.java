package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.EmailInvitationResponse;
import com.project.projectmanagementapplication.dto.InvitationAcceptanceResponse;
import com.project.projectmanagementapplication.dto.ProjectDetailsResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.INVITATION_STATUS;
import com.project.projectmanagementapplication.exception.*;
import com.project.projectmanagementapplication.model.Invitation;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.InvitationRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationServiceImpl implements InvitationService {

    @Value("${app.invitation.expiry-hours}")
    private int expiryHours;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final InvitationRepository invitationRepository;

    private final EmailService emailService;

    private final UserService userService;

    private final ProjectService projectService;

    @Autowired
    public InvitationServiceImpl(InvitationRepository invitationRepository, EmailService emailService, UserService userService, ProjectService projectService) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.userService = userService;
        this.projectService = projectService;
    }

    private String generateInvitationToken(String email, Long projectId) {
        return java.util.UUID.randomUUID().toString();
    }

    private Invitation createInvitation(String email, Long projectId, String invitationToken) {
        Invitation invitation = new Invitation();
        invitation.setEmail(email);
        invitation.setProjectId(projectId);
        invitation.setToken(invitationToken);
        return invitation;
    }


    @Override
    public Response<EmailInvitationResponse> sendInvitation(String email, Long userId, Long projectId, boolean forceResend) throws MessagingException {
        // Validation
        validateInvitationRequest(email, userId, projectId);

        // Check for existing invitation
        Optional<Invitation> existingInvitation = invitationRepository
                .findByEmailAndProjectIdAndStatus(email, projectId, INVITATION_STATUS.PENDING);

        // Handle existing invitation logic
        if (existingInvitation.isPresent() && !existingInvitation.get().isExpired() && !forceResend) {
            throw new InvitationAlreadySentException(email, projectId);
        }

        // Create or update invitation
        Invitation invitation = createOrUpdateInvitation(existingInvitation, email, userId, projectId);

        // Send email
        sendInvitationEmail(invitation, email);

        // Build response
        return buildSuccessResponse(invitation, email, existingInvitation.isPresent());
    }

    private void validateInvitationRequest(String email, Long userId, Long projectId) {
        User inviter = userService.findByUserId(userId);
        Response<Project> projectResponse = projectService.getProjectById(projectId);
        Project project = projectResponse.getData();

        if (!inviter.getId().equals(project.getOwner().getId())) {
            throw new UnauthorizedException("Only project owner can send invitations");
        }

        boolean isAlreadyMember = project.getTeam().stream()
                .anyMatch(member -> email.equalsIgnoreCase(member.getEmail()));

        if (isAlreadyMember) {
            throw new ConflictException("User is already a member of this project");
        }
    }

    private Invitation createOrUpdateInvitation(Optional<Invitation> existingInvitation,
                                                String email, Long userId, Long projectId) {
        String invitationToken = generateInvitationToken();
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(expiryHours);

        if (existingInvitation.isPresent()) {
            Invitation invitation = existingInvitation.get();
            invitation.setToken(invitationToken);
            invitation.setStatus(INVITATION_STATUS.PENDING);
            invitation.setExpiresAt(expiryTime);
            return invitationRepository.save(invitation);
        } else {
            Invitation invitation = new Invitation();
            invitation.setInvitedBy(userId);
            invitation.setProjectId(projectId);
            invitation.setEmail(email);
            invitation.setToken(invitationToken);
            invitation.setStatus(INVITATION_STATUS.PENDING);
            invitation.setExpiresAt(expiryTime);
            return invitationRepository.save(invitation);
        }
    }

    private void sendInvitationEmail(Invitation invitation, String email) throws MessagingException {
        //String invitationLink = "http://localhost:5173/accept_invitation?token=" + invitation.getToken();
        String invitationLink = frontendBaseUrl + "/accept_invitation?token=" + invitation.getToken();

        emailService.sendEmailWithToken(email, invitationLink);
    }

    private Response<EmailInvitationResponse> buildSuccessResponse(Invitation invitation, String email, boolean isResend) {
        String message = isResend ?
                "Invitation resent successfully to " + email :
                "Invitation sent successfully to " + email;

        return Response.<EmailInvitationResponse>builder()
                .data(EmailInvitationResponse.builder()
                        .invitationId(invitation.getId())
                        .isResend(isResend)
                        .email(email)
                        .build())
                .message(message)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    private String generateInvitationToken() {
        return UUID.randomUUID().toString();
    }
    @Override
    public Response<Invitation> acceptInvitation(String token, Long userId) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new Exception("Invitation not found for token: " + token);
        }
        return Response.<Invitation>builder()
                .data(invitation)
                .message("Invitation accepted successfully")
                .status(HttpStatus.ACCEPTED)
                .statusCode(HttpStatus.ACCEPTED.value())
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();
    }

    @Override
    public String getTokenByUserMail(String userEmail) throws Exception {
        Invitation invitation = invitationRepository.findByEmail(userEmail);
        if (invitation == null) {
            throw new Exception("Invitation not found for email: " + userEmail);
        }
        return invitation.getToken();
    }

    @Override
    public void deleteToken(String token) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new Exception("Invitation not found for token: " + token);
        }
        invitationRepository.delete(invitation);
    }

    @Override
    public Response<ProjectDetailsResponse> getInvitationDetails(String token) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);

        if (invitation == null) {
            throw new ResourceNotFoundException("Invitation not found for token: " + token);
        }

        if (invitation.isExpired()) {
            invitation.setStatus(INVITATION_STATUS.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvitationExpiredException("Invitation has expired");
        }

        if (invitation.getStatus() != INVITATION_STATUS.PENDING) {
            throw new ConflictException("Invitation has already been " + invitation.getStatus().toString().toLowerCase());
        }

        Response<Project> projectResponse = projectService.getProjectById(invitation.getProjectId());
        Project project = projectResponse.getData();

        ProjectDetailsResponse projectDetails = ProjectDetailsResponse.builder()
                .projectName(project.getName())
                .projectDescription(project.getDescription())
                .projectCategory(project.getCategory())
                .ownerName(project.getOwner().getFirstName())
                .teamSize(project.getTeam().size())
                .token(token)
                .build();

        return Response.<ProjectDetailsResponse>builder()
                .data(projectDetails)
                .message("Invitation details retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<InvitationAcceptanceResponse> processInvitationAcceptance(String token, String userEmail) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);

        if (invitation == null) {
            throw new ResourceNotFoundException("Invitation not found for token: " + token);
        }

        if (invitation.isExpired()) {
            invitation.setStatus(INVITATION_STATUS.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvitationExpiredException("Invitation has expired");
        }

        if (invitation.getStatus() != INVITATION_STATUS.PENDING) {
            throw new ConflictException("Invitation has already been " + invitation.getStatus().toString().toLowerCase());
        }

        // Verify email matches invitation
        if (!invitation.getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedException("Email does not match invitation recipient");
        }

        // Get project details
        Response<Project> projectResponse = projectService.getProjectById(invitation.getProjectId());
        Project project = projectResponse.getData();

        // Check if user exists in system
        User user = null;
        boolean userExists = false;
        boolean isNewUser = false;

        try {
            user = userService.findByUsername(userEmail);
            userExists = true;

            // Check if user is already in project
            if (project.getTeam().contains(user)) {
                throw new ConflictException("User is already a member of this project");
            }

            // If user exists, add them to project immediately
            projectService.addUserToProject(user.getId(), invitation.getProjectId());

            // Mark invitation as accepted - THIS WAS MISSING SAVE!
            invitation.setStatus(INVITATION_STATUS.ACCEPTED);
            invitationRepository.save(invitation);

        } catch (ResourceNotFoundException e) {
            // User doesn't exist in system
            userExists = false;
            isNewUser = true;

            invitation.setUpdatedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
        }

        InvitationAcceptanceResponse response = InvitationAcceptanceResponse.builder()
                .message("Invitation processed successfully")
                .projectName(project.getName())
                .projectId(project.getId())
                .userEmail(userEmail)
                .userExists(userExists)
                .isNewUser(isNewUser)
                .redirectUrl("/dashboard")
                .build();

        return Response.<InvitationAcceptanceResponse>builder()
                .data(response)
                .message("Invitation acceptance processed successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<Void> acceptInvitationAfterRegistration(String userEmail) throws Exception {
        // Find pending invitation for this email
        List<Invitation> pendingInvitations = invitationRepository
                .findByEmailAndStatus(userEmail, INVITATION_STATUS.PENDING);

        if (pendingInvitations.isEmpty()) {
            return Response.<Void>builder()
                    .message("No pending invitations found")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        User user = userService.findByUsername(userEmail);

        // Process all pending invitations for this user
        for (Invitation invitation : pendingInvitations) {
            if (!invitation.isExpired()) {
                projectService.addUserToProject(user.getId(), invitation.getProjectId());
                invitation.setStatus(INVITATION_STATUS.ACCEPTED);
                invitationRepository.save(invitation);
            } else {
                invitation.setStatus(INVITATION_STATUS.EXPIRED);
                invitationRepository.save(invitation);
            }
        }

        return Response.<Void>builder()
                .message("Pending invitations processed successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
