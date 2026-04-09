package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.model.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {

    Optional<ProjectMembership> findByProject_IdAndUser_Id(Long projectId, Long userId);

    List<ProjectMembership> findByProject_Id(Long projectId);

    boolean existsByProject_IdAndRole(Long projectId, PROJECT_MEMBER_ROLE role);

    Optional<ProjectMembership> findByProject_IdAndRole(Long projectId, PROJECT_MEMBER_ROLE role);

    void deleteByProject_IdAndUser_Id(Long projectId, Long userId);
}
