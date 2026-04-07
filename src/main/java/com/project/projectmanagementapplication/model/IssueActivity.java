package com.project.projectmanagementapplication.model;

import com.project.projectmanagementapplication.enums.IssueActivityType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_activity", indexes = @Index(name = "idx_issue_activity_issue_created", columnList = "issue_id,created_at"))
@Data
public class IssueActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private IssueActivityType activityType;

    /** For {@link IssueActivityType#TASK_FIELD_UPDATED}; null for {@link IssueActivityType#TASK_CREATED}. */
    @Column(name = "field_name", length = 64)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
