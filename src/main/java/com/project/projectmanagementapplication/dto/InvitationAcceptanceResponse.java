package com.project.projectmanagementapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationAcceptanceResponse {
    private String message;
    private String projectName;
    private Long projectId;
    private String userEmail;
    private boolean userExists;
    private boolean isNewUser;
    private String redirectUrl;
}
