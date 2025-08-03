package com.project.projectmanagementapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailInvitationResponse {
    private Long invitationId;
    private boolean isResend;
    private String email;
    private boolean canResend;
}