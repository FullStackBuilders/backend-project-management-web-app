package com.project.projectmanagementapplication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Issue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private ISSUE_STATUS status;

    @Enumerated(EnumType.STRING)
    private ISSUE_PRIORITY priority;

    private LocalDate AssignedDate;

    private LocalDate dueDate;

    @ManyToOne
    private User assignee;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @JsonIgnore
    @ManyToOne
    private Project project;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "last_edited_by_id")
    private User lastEditedBy;

    private LocalDateTime lastEditedAt;

    // Workflow timestamps for Kanban metrics — separate from generic audit fields
    private LocalDateTime taskStartedAt;    // set when task first enters IN_PROGRESS; never overwritten
    private LocalDateTime taskCompletedAt;  // set when task enters DONE; cleared when task is reopened

    @JsonIgnore
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

}
