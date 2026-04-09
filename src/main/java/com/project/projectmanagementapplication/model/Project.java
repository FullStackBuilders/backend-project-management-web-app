package com.project.projectmanagementapplication.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Project extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "framework", nullable = false, length = 32)
    private PROJECT_FRAMEWORK framework = PROJECT_FRAMEWORK.KANBAN;

    @ElementCollection
    private List<String> tags = new ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL,orphanRemoval = true)
    private Chat chat;

    @ManyToOne
    private User owner;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Sprint> sprints = new ArrayList<>();

    @ManyToMany
    private List<User> team = new ArrayList<>();

    @Transient
    private String myRole;

    @Transient
    private Boolean canEditProject;

    @Transient
    private Boolean canDeleteProject;

    @Transient
    private Boolean canInviteMembers;

    @Transient
    private Boolean canManageAllTasks;
}
