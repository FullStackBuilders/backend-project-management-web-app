package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Invitation;

public interface InvitationService {

    public Response<Void> sendInvitation(String email, Long projectId) throws Exception;

    public Response<Invitation> acceptInvitation(String token, Long userId) throws Exception;

    public String getTokenByUserMail(String userEmail) throws Exception;

    void deleteToken(String token) throws Exception;
}
