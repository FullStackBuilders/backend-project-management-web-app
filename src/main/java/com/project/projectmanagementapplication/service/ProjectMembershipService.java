package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.ConflictException;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.ProjectMembership;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.ProjectMembershipRepository;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ProjectMembershipService {

    private final ProjectMembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    public ProjectMembershipService(
            ProjectMembershipRepository membershipRepository,
            ProjectRepository projectRepository,
            UserService userService) {
        this.membershipRepository = membershipRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public void createOwnerMembership(Project project, User owner) {
        if (membershipRepository.findByProject_IdAndUser_Id(project.getId(), owner.getId()).isPresent()) {
            return;
        }
        ProjectMembership m = new ProjectMembership();
        m.setProject(project);
        m.setUser(owner);
        m.setRole(PROJECT_MEMBER_ROLE.OWNER);
        membershipRepository.save(m);
    }

    public void createMemberMembership(Project project, User user) {
        if (membershipRepository.findByProject_IdAndUser_Id(project.getId(), user.getId()).isPresent()) {
            return;
        }
        ProjectMembership m = new ProjectMembership();
        m.setProject(project);
        m.setUser(user);
        m.setRole(PROJECT_MEMBER_ROLE.MEMBER);
        membershipRepository.save(m);
    }

    public void removeMembership(Long projectId, Long userId) {
        membershipRepository.deleteByProject_IdAndUser_Id(projectId, userId);
    }

    /**
     * Resolved role from {@code project_membership}, or legacy {@link Project#getOwner()} / {@link Project#getTeam()}.
     */
    public PROJECT_MEMBER_ROLE getRole(Long projectId, Long userId) {
        return membershipRepository
                .findByProject_IdAndUser_Id(projectId, userId)
                .map(ProjectMembership::getRole)
                .orElseGet(
                        () -> {
                            Project project =
                                    projectRepository
                                            .findById(projectId)
                                            .orElseThrow(
                                                    () ->
                                                            new ResourceNotFoundException(
                                                                    "Project not found with ID: "
                                                                            + projectId));
                            User user = userService.findByUserId(userId);
                            PROJECT_MEMBER_ROLE role;
                            if (project.getOwner().getId().equals(userId)) {
                                role = PROJECT_MEMBER_ROLE.OWNER;
                            } else if (isOnTeam(project, userId)) {
                                role = PROJECT_MEMBER_ROLE.MEMBER;
                            } else {
                                throw new UnauthorizedException("You are not a member of this project");
                            }
                            ProjectMembership m = new ProjectMembership();
                            m.setProject(project);
                            m.setUser(user);
                            m.setRole(role);
                            membershipRepository.save(m);
                            return role;
                        });
    }

    private static boolean isOnTeam(Project project, Long userId) {
        if (project.getTeam() == null) {
            return false;
        }
        return project.getTeam().stream().anyMatch(u -> u.getId().equals(userId));
    }

    public void backfillAllProjects() {
        List<Project> projects = projectRepository.findAll();
        for (Project p : projects) {
            Long pid = p.getId();
            User owner = p.getOwner();
            if (owner != null
                    && membershipRepository.findByProject_IdAndUser_Id(pid, owner.getId()).isEmpty()) {
                createOwnerMembership(p, owner);
            }
            if (p.getTeam() != null) {
                for (User u : p.getTeam()) {
                    if (membershipRepository.findByProject_IdAndUser_Id(pid, u.getId()).isEmpty()) {
                        if (owner != null && u.getId().equals(owner.getId())) {
                            continue;
                        }
                        createMemberMembership(p, u);
                    }
                }
            }
        }
    }

    /**
     * Update another member's role. Caller must be OWNER or ADMIN (validated elsewhere).
     */
    public PROJECT_MEMBER_ROLE updateMemberRole(
            Project project, User caller, Long targetUserId, PROJECT_MEMBER_ROLE newRole) {
        Objects.requireNonNull(newRole);
        userService.findByUserId(targetUserId);
        getRole(project.getId(), targetUserId);
        PROJECT_MEMBER_ROLE callerRole = getRole(project.getId(), caller.getId());

        if (callerRole != PROJECT_MEMBER_ROLE.OWNER && callerRole != PROJECT_MEMBER_ROLE.ADMIN) {
            throw new UnauthorizedException("Only project owner or admin can change member roles");
        }

        if (targetUserId.equals(project.getOwner().getId())) {
            if (newRole != PROJECT_MEMBER_ROLE.OWNER) {
                throw new BadRequestException("Cannot change the project owner's role");
            }
            return PROJECT_MEMBER_ROLE.OWNER;
        }

        if (callerRole == PROJECT_MEMBER_ROLE.ADMIN) {
            PROJECT_MEMBER_ROLE targetCurrent = getRole(project.getId(), targetUserId);
            if (targetCurrent == PROJECT_MEMBER_ROLE.OWNER) {
                throw new UnauthorizedException("Admins cannot modify the project owner");
            }
        }

        if (newRole == PROJECT_MEMBER_ROLE.SCRUM_MASTER) {
            if (project.getFramework() != com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK.SCRUM) {
                throw new BadRequestException("Scrum Master role is only valid for Scrum projects");
            }
            assertNoConflictingScrumMaster(project, targetUserId);
        }

        ProjectMembership m = membershipRepository
                .findByProject_IdAndUser_Id(project.getId(), targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this project"));

        if (m.getRole() == PROJECT_MEMBER_ROLE.OWNER) {
            throw new BadRequestException("Cannot reassign owner row via this operation");
        }

        m.setRole(newRole);
        membershipRepository.save(m);
        return newRole;
    }

    private void assertNoConflictingScrumMaster(Project project, Long targetUserId) {
        membershipRepository
                .findByProject_IdAndRole(project.getId(), PROJECT_MEMBER_ROLE.SCRUM_MASTER)
                .ifPresent(
                        existing -> {
                            if (!existing.getUser().getId().equals(targetUserId)) {
                                throw new ConflictException(
                                        "A Scrum Master is already assigned to this project. Only one Scrum Master is allowed.");
                            }
                        });
    }
}

