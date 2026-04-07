package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.IssueFieldChangeSnapshot;
import com.project.projectmanagementapplication.enums.IssueActivityType;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.IssueActivity;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class IssueActivityRecorder {

    private final IssueActivityRepository issueActivityRepository;

    public IssueActivityRecorder(IssueActivityRepository issueActivityRepository) {
        this.issueActivityRepository = issueActivityRepository;
    }

    @Transactional
    public void recordTaskCreated(Issue issue, User actor) {
        LocalDateTime at = LocalDateTime.now();
        IssueActivity row = new IssueActivity();
        row.setIssue(issue);
        row.setActor(actor);
        row.setActivityType(IssueActivityType.TASK_CREATED);
        row.setFieldName(null);
        row.setOldValue(null);
        row.setNewValue(null);
        row.setCreatedAt(at);
        issueActivityRepository.save(row);
    }

    @Transactional
    public void recordFieldUpdates(Issue issue, User actor, List<IssueFieldChangeSnapshot> changes, LocalDateTime occurredAt) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        List<IssueActivity> rows = new ArrayList<>();
        for (IssueFieldChangeSnapshot c : changes) {
            IssueActivity row = new IssueActivity();
            row.setIssue(issue);
            row.setActor(actor);
            row.setActivityType(IssueActivityType.TASK_FIELD_UPDATED);
            row.setFieldName(c.fieldName());
            row.setOldValue(c.oldValue());
            row.setNewValue(c.newValue());
            row.setCreatedAt(occurredAt);
            rows.add(row);
        }
        issueActivityRepository.saveAll(rows);
    }

    @Transactional
    public void recordSingleFieldUpdate(
            Issue issue,
            User actor,
            String fieldName,
            String oldValue,
            String newValue,
            LocalDateTime occurredAt) {
        IssueActivity row = new IssueActivity();
        row.setIssue(issue);
        row.setActor(actor);
        row.setActivityType(IssueActivityType.TASK_FIELD_UPDATED);
        row.setFieldName(fieldName);
        row.setOldValue(oldValue);
        row.setNewValue(newValue);
        row.setCreatedAt(occurredAt);
        issueActivityRepository.save(row);
    }
}
