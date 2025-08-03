package com.project.projectmanagementapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationConflictDetails {
    private String email;
    private Long projectId;
    private boolean canResend;
    private String existingInvitationId;
}
