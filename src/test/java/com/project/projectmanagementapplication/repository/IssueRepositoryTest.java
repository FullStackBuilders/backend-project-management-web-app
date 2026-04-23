package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class IssueRepositoryTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SprintRepository sprintRepository;

    private Project project;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        sprintRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setFirstName("O");
        owner.setLastName("wner");
        owner.setPassword("pw");
        owner = userRepository.save(owner);

        project = new Project();
        project.setName("Demo");
        project.setDescription("D");
        project.setCategory("Cat");
        project.setOwner(owner);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.getTeam().add(owner);
        project = projectRepository.save(project);

        sprint = new Sprint();
        sprint.setName("S1");
        sprint.setStatus(SPRINT_STATUS.ACTIVE);
        sprint.setProject(project);
        sprint = sprintRepository.save(sprint);
    }

    @Test
    void findByProjectId_returnsIssuesForProject() {
        Issue a = new Issue();
        a.setTitle("A");
        a.setStatus(ISSUE_STATUS.TO_DO);
        a.setPriority(ISSUE_PRIORITY.MEDIUM);
        a.setProject(project);
        issueRepository.save(a);

        List<Issue> found = issueRepository.findByProjectId(project.getId());
        assertEquals(1, found.size());
        assertEquals("A", found.get(0).getTitle());
    }

    @Test
    void countByProjectIdAndStatus_countsMatchingStatus() {
        Issue todo = new Issue();
        todo.setTitle("t1");
        todo.setStatus(ISSUE_STATUS.TO_DO);
        todo.setPriority(ISSUE_PRIORITY.LOW);
        todo.setProject(project);
        issueRepository.save(todo);

        Issue wip = new Issue();
        wip.setTitle("w1");
        wip.setStatus(ISSUE_STATUS.IN_PROGRESS);
        wip.setPriority(ISSUE_PRIORITY.LOW);
        wip.setProject(project);
        issueRepository.save(wip);

        assertEquals(1L, issueRepository.countByProjectIdAndStatus(project.getId(), ISSUE_STATUS.TO_DO));
        assertEquals(1L, issueRepository.countByProjectIdAndStatus(project.getId(), ISSUE_STATUS.IN_PROGRESS));
    }

    @Test
    void findByProjectIdAndSprintIsNull_onlyBacklogIssues() {
        Issue backlog = new Issue();
        backlog.setTitle("backlog");
        backlog.setStatus(ISSUE_STATUS.TO_DO);
        backlog.setPriority(ISSUE_PRIORITY.LOW);
        backlog.setProject(project);
        backlog.setSprint(null);
        issueRepository.save(backlog);

        Issue inSprint = new Issue();
        inSprint.setTitle("in sprint");
        inSprint.setStatus(ISSUE_STATUS.TO_DO);
        inSprint.setPriority(ISSUE_PRIORITY.LOW);
        inSprint.setProject(project);
        inSprint.setSprint(sprint);
        issueRepository.save(inSprint);

        List<Issue> backlogOnly = issueRepository.findByProjectIdAndSprintIsNull(project.getId());
        assertEquals(1, backlogOnly.size());
        assertEquals("backlog", backlogOnly.get(0).getTitle());
    }

    @Test
    void countBySprint_Id_countsIssuesInSprint() {
        Issue i = new Issue();
        i.setTitle("s");
        i.setStatus(ISSUE_STATUS.DONE);
        i.setPriority(ISSUE_PRIORITY.HIGH);
        i.setProject(project);
        i.setSprint(sprint);
        issueRepository.save(i);

        assertEquals(1L, issueRepository.countBySprint_Id(sprint.getId()));
    }

    @Test
    void findBySprint_Id_returnsIssues() {
        Issue i = new Issue();
        i.setTitle("in");
        i.setStatus(ISSUE_STATUS.IN_PROGRESS);
        i.setPriority(ISSUE_PRIORITY.MEDIUM);
        i.setProject(project);
        i.setSprint(sprint);
        issueRepository.save(i);

        assertTrue(issueRepository.findBySprint_Id(sprint.getId()).stream().anyMatch(x -> "in".equals(x.getTitle())));
    }
}
