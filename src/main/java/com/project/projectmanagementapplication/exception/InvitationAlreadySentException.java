package com.project.projectmanagementapplication.exception;

import lombok.Getter;

@Getter
public class InvitationAlreadySentException extends RuntimeException{
    private final String email;
    private final Long projectId;

    public InvitationAlreadySentException(String email, Long projectId) {
        super(String.format("Invitation already sent to %s for project %d", email, projectId));
        this.email = email;
        this.projectId = projectId;
    }
}
