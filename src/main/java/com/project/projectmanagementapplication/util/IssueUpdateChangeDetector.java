package com.project.projectmanagementapplication.util;

import com.project.projectmanagementapplication.constants.IssueTaskFieldNames;
import com.project.projectmanagementapplication.dto.IssueFieldChangeSnapshot;
import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.model.Issue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Detects meaningful field changes for issue edit form updates (not assignee/status endpoints).
 */
public final class IssueUpdateChangeDetector {

    private IssueUpdateChangeDetector() {}

    public static String normalizeTitle(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    public static String normalizeDescription(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    public static String titleDisplay(String s) {
        String n = normalizeTitle(s);
        return n.isEmpty() ? null : n;
    }

    public static String descriptionDisplay(String s) {
        String n = normalizeDescription(s);
        return n.isEmpty() ? null : n;
    }

    public static String priorityDisplay(ISSUE_PRIORITY p) {
        if (p == null) {
            return null;
        }
        return switch (p) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
        };
    }

    public static String dueDateDisplay(LocalDate d) {
        return d == null ? null : d.toString();
    }

    /**
     * Returns a list of field-level snapshots for every meaningful change.
     * Caller must validate {@code request.getPriority()} when non-null before calling.
     */
    public static List<IssueFieldChangeSnapshot> detect(Issue issue, IssueRequest request) {
        List<IssueFieldChangeSnapshot> changes = new ArrayList<>();

        if (!normalizeTitle(issue.getTitle()).equals(normalizeTitle(request.getTitle()))) {
            changes.add(new IssueFieldChangeSnapshot(
                    IssueTaskFieldNames.TITLE,
                    titleDisplay(issue.getTitle()),
                    titleDisplay(request.getTitle())));
        }

        if (!normalizeDescription(issue.getDescription()).equals(normalizeDescription(request.getDescription()))) {
            changes.add(new IssueFieldChangeSnapshot(
                    IssueTaskFieldNames.DESCRIPTION,
                    descriptionDisplay(issue.getDescription()),
                    descriptionDisplay(request.getDescription())));
        }

        if (request.getPriority() != null) {
            ISSUE_PRIORITY newPriority = ISSUE_PRIORITY.valueOf(request.getPriority());
            if (issue.getPriority() != newPriority) {
                changes.add(new IssueFieldChangeSnapshot(
                        IssueTaskFieldNames.PRIORITY,
                        priorityDisplay(issue.getPriority()),
                        priorityDisplay(newPriority)));
            }
        }

        if (!Objects.equals(issue.getDueDate(), request.getDueDate())) {
            changes.add(new IssueFieldChangeSnapshot(
                    IssueTaskFieldNames.DUE_DATE,
                    dueDateDisplay(issue.getDueDate()),
                    dueDateDisplay(request.getDueDate())));
        }

        return changes;
    }
}
