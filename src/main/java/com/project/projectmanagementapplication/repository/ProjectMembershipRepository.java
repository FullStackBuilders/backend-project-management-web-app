package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.model.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {

    Optional<ProjectMembership> findByProject_IdAndUser_Id(Long projectId, Long userId);

    List<ProjectMembership> findByProject_Id(Long projectId);

    boolean existsByProject_IdAndRole(Long projectId, PROJECT_MEMBER_ROLE role);

    Optional<ProjectMembership> findByProject_IdAndRole(Long projectId, PROJECT_MEMBER_ROLE role);

    void deleteByProject_IdAndUser_Id(Long projectId, Long userId);

    @Query("SELECT m.user.id FROM ProjectMembership m WHERE m.project.id = :projectId AND m.role = :role")
    Optional<Long> findUserIdByProjectIdAndRole(@Param("projectId") Long projectId,
                                                @Param("role") PROJECT_MEMBER_ROLE role);
}
