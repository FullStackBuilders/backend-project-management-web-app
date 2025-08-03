package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.INVITATION_STATUS;
import com.project.projectmanagementapplication.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Invitation findByToken(String token);

    Invitation findByEmail(String userEmail);

    Optional<Invitation> findByEmailAndProjectIdAndStatus(String email, Long projectId, INVITATION_STATUS status);

    List<Invitation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Invitation> findByStatusAndExpiresAtBefore(INVITATION_STATUS status, LocalDateTime dateTime);

    List<Invitation> findByEmailAndStatus(String email, INVITATION_STATUS status);

    // Count pending invitations for a project
    long countByProjectIdAndStatus(Long projectId, INVITATION_STATUS status);

    // Find all invitations by a specific user
    List<Invitation> findByInvitedByOrderByCreatedAtDesc(Long invitedBy);
}
