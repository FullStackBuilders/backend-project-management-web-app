package com.project.projectmanagementapplication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "project_membership",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"})
)
@Data
@NoArgsConstructor
public class ProjectMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PROJECT_MEMBER_ROLE role;
}
