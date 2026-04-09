package com.project.projectmanagementapplication.enums;

/**
 * Per-project membership role. Scrum-only {@link #SCRUM_MASTER}; at most one per Scrum project.
 */
public enum PROJECT_MEMBER_ROLE {
    OWNER,
    ADMIN,
    MEMBER,
    SCRUM_MASTER
}
