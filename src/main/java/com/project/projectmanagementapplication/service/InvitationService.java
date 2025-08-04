package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.EmailInvitationResponse;
import com.project.projectmanagementapplication.dto.InvitationAcceptanceResponse;
import com.project.projectmanagementapplication.dto.ProjectDetailsResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Invitation;
import jakarta.mail.MessagingException;

public interface InvitationService {

    public Response<EmailInvitationResponse> sendInvitation(String email, Long userId, Long projectId, boolean forceResend) throws MessagingException;

    public Response<Invitation> acceptInvitation(String token, Long userId) throws Exception;

    public String getTokenByUserMail(String userEmail) throws Exception;

    void deleteToken(String token) throws Exception;

    Response<InvitationAcceptanceResponse> processInvitationAcceptance(String token, String userEmail) throws Exception;

    Response<ProjectDetailsResponse> getInvitationDetails(String token) throws Exception;

    Response<Void> acceptInvitationAfterRegistration(String userEmail) throws Exception;
}
