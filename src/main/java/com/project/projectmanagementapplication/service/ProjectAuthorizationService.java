package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import org.springframework.stereotype.Service;

@Service
public class ProjectAuthorizationService {

    private final ProjectMembershipService membershipService;

    public ProjectAuthorizationService(ProjectMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public PROJECT_MEMBER_ROLE getRole(Long projectId, User user) {
        return membershipService.getRole(projectId, user.getId());
    }

    /** Full edit/delete power over any task in the project. */
    public boolean canAdministerAllTasks(Long projectId, User user) {
        PROJECT_MEMBER_ROLE r = getRole(projectId, user);
        return r == PROJECT_MEMBER_ROLE.OWNER || r == PROJECT_MEMBER_ROLE.ADMIN;
    }

    public boolean canInviteToProject(Long projectId, User user) {
        PROJECT_MEMBER_ROLE r = getRole(projectId, user);
        return r == PROJECT_MEMBER_ROLE.OWNER || r == PROJECT_MEMBER_ROLE.ADMIN;
    }

    /** Sprint create/edit/start/complete (Scrum). */
    public boolean canManageSprints(Project project, User user) {
        if (project.getFramework() != PROJECT_FRAMEWORK.SCRUM) {
            return false;
        }
        PROJECT_MEMBER_ROLE r = getRole(project.getId(), user);
        return r == PROJECT_MEMBER_ROLE.OWNER
                || r == PROJECT_MEMBER_ROLE.ADMIN
                || r == PROJECT_MEMBER_ROLE.SCRUM_MASTER;
    }

    /** Mutate project settings or delete project. */
    public boolean canManageProjectSettings(Long projectId, User user) {
        PROJECT_MEMBER_ROLE r = getRole(projectId, user);
        return r == PROJECT_MEMBER_ROLE.OWNER || r == PROJECT_MEMBER_ROLE.ADMIN;
    }

    /** MEMBER sees filtered sprint list (ACTIVE+COMPLETED, no INACTIVE). */
    public boolean usesMemberSprintListFilter(Long projectId, User user) {
        return getRole(projectId, user) == PROJECT_MEMBER_ROLE.MEMBER;
    }

    /** MEMBER may only assign issues to ACTIVE sprints (non-null target). */
    public boolean mustRestrictSprintAssignmentToActive(Long projectId, User user) {
        return getRole(projectId, user) == PROJECT_MEMBER_ROLE.MEMBER;
    }

    /** Status updates: treat ADMIN like owner for authorization checks. */
    public boolean canActAsProjectAdminForIssue(User user, Project project) {
        PROJECT_MEMBER_ROLE r = getRole(project.getId(), user);
        return r == PROJECT_MEMBER_ROLE.OWNER || r == PROJECT_MEMBER_ROLE.ADMIN;
    }
}
