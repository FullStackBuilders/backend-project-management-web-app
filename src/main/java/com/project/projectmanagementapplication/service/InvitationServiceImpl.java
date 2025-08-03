package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.EmailInvitationResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.INVITATION_STATUS;
import com.project.projectmanagementapplication.exception.ConflictException;
import com.project.projectmanagementapplication.exception.InvitationAlreadySentException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
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
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationServiceImpl implements InvitationService {

    @Value("${app.invitation.expiry-hours}")
    private int expiryHours;

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
        String invitationLink = "http://localhost:5173/accept_invitation?token=" + invitation.getToken();
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
}
