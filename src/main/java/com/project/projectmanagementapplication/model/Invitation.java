package com.project.projectmanagementapplication.model;

import com.project.projectmanagementapplication.enums.INVITATION_STATUS;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Invitation extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private INVITATION_STATUS status = INVITATION_STATUS.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

}
