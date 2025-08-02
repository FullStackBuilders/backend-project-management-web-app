package com.project.projectmanagementapplication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Issue {

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

    @JsonIgnore
    @ManyToOne
    private Project project;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @JsonIgnore
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

}
