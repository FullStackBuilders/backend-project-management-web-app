package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Invitation;
import com.project.projectmanagementapplication.repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InvitationServiceImpl implements InvitationService {


    private final InvitationRepository invitationRepository;

    private final EmailService emailService;

    @Autowired
    public InvitationServiceImpl(InvitationRepository invitationRepository, EmailService emailService) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
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
    public Response<Void> sendInvitation(String email, Long projectId) throws Exception {
        String  invitationToken = generateInvitationToken(email, projectId);

        Invitation invitation = createInvitation(email, projectId, invitationToken);

        invitationRepository.save(invitation);

        String invitationLink = "http://localhost:5173/accept_invitation?token=" +invitationToken;

        emailService.sendEmailWithToken(email, invitationLink);

        return Response.<Void>builder()
                .message("Invitation sent successfully to " + email)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();


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
