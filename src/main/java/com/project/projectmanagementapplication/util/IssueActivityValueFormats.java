package com.project.projectmanagementapplication.util;

import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;

public final class IssueActivityValueFormats {

    private IssueActivityValueFormats() {}

    public static String statusDisplay(ISSUE_STATUS s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case TO_DO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Done";
        };
    }

    public static String assigneeDisplay(User u) {
        if (u == null) {
            return null;
        }
        return u.getFirstName() + " " + u.getLastName();
    }

    public static String sprintDisplay(Sprint sprint) {
        if (sprint == null) {
            return "Backlog";
        }
        return sprint.getName() != null ? sprint.getName() : "Sprint";
    }
}
