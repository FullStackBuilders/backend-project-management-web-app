package com.project.projectmanagementapplication.mocks;

import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class MockProjectFactory {
    public static Project createMockProject(Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setName("Test project");
        project.setDescription("This is a test project!");
        project.setCategory("Full Stack");
        project.setTags(Arrays.asList("python", "javascript", "django", "machine learning", "docker"));

        User owner = new User();
        owner.setId(1L);
        owner.setFirstName("Scarlett");
        owner.setLastName("Doe");
        owner.setEmail("scarlett@example.com");
        project.setOwner(owner);

        // Team Members
        List<User> team = Arrays.asList(
                owner,
                createUser(2L, "John", "Doe", "john@example.com"),
                createUser(3L, "Alice", "Doe", "alicek@example.com"),
                createUser(4L, "Nick", "Doe", "nick@example.com"),
                createUser(5L, "Bob", "Deep", "bob@example.com"),
                createUser(6L, "Mick", "Doe", "mick@example.com"),
                createUser(7L, "Lee", "Doe", "lee@example.com"),
                createUser(8L, "ralf", "Doe", "ralf@example.com")
        );
        project.setTeam(team);

        // Issues
        Issue issue1 = new Issue();
        issue1.setId(1L);
        issue1.setTitle("Add Navbar");
        issue1.setDescription("Create navbar Component");
        issue1.setStatus(ISSUE_STATUS.DONE);
        issue1.setPriority(ISSUE_PRIORITY.MEDIUM);
        issue1.setDueDate(LocalDate.of(2025, 8, 5));
        issue1.setAssignedDate(LocalDate.of(2025, 8, 3));
        issue1.setAssignee(owner);
        issue1.setCreatedBy(team.get(7)); // User2
        issue1.setProject(project);

        Issue issue2 = new Issue();
        issue2.setId(2L);
        issue2.setTitle("Add footer to home page");
        issue2.setDescription("add footer to home page");
        issue2.setStatus(ISSUE_STATUS.DONE);
        issue2.setPriority(ISSUE_PRIORITY.HIGH);
        issue2.setDueDate(LocalDate.of(2025, 8, 27));
        issue2.setAssignedDate(LocalDate.of(2025, 8, 3));
        issue2.setAssignee(team.get(5)); // Snehal
        issue2.setCreatedBy(team.get(1)); // John
        issue2.setProject(project);

        project.setIssues(Arrays.asList(issue1, issue2));

        return project;
    }

    private static User createUser(Long id, String first, String last, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setProjectSize(1);
        return user;
    }
}
