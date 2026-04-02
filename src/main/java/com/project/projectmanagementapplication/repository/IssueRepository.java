package com.project.projectmanagementapplication.repository;


import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByProjectId(Long projectId);

    long countByAssigneeAndProjectIn(User assignee, List<Project> projects);

    long countByAssigneeAndProjectInAndDueDateBeforeAndStatusNot(
            User assignee,
            List<Project> projects,
            LocalDate date,
            ISSUE_STATUS status
    );

    long countByAssigneeAndProjectInAndPriorityAndStatusNot(
            User assignee,
            List<Project> projects,
            ISSUE_PRIORITY priority,
            ISSUE_STATUS status
    );

    long countByAssigneeAndProjectInAndStatus(
            User assignee,
            List<Project> projects,
            ISSUE_STATUS status
    );

    long countByAssigneeAndProjectInAndDueDateAndStatusNot(
            User assignee,
            List<Project> projects,
            LocalDate date,
            ISSUE_STATUS status
    );

}
