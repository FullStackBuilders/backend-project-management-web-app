package com.project.projectmanagementapplication.integration.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_PRIORITY;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.PROJECT_MEMBER_ROLE;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.ProjectMembership;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.BoardColumnSettingRepository;
import com.project.projectmanagementapplication.repository.ChatRepository;
import com.project.projectmanagementapplication.repository.CommentRepository;
import com.project.projectmanagementapplication.repository.InvitationRepository;
import com.project.projectmanagementapplication.repository.IssueActivityRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.MessageRepository;
import com.project.projectmanagementapplication.repository.ProjectMembershipRepository;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import com.project.projectmanagementapplication.repository.SprintRepository;
import com.project.projectmanagementapplication.repository.SubscriptionRepository;
import com.project.projectmanagementapplication.repository.UserRepository;
import com.project.projectmanagementapplication.security.JwtUtil;
import com.project.projectmanagementapplication.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

@TestPropertySource(
        properties = {
            "jwt.secretKey=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        })
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected ProjectService projectService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    protected SprintRepository sprintRepository;

    @Autowired
    protected IssueRepository issueRepository;

    @Autowired
    protected IssueActivityRepository issueActivityRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected InvitationRepository invitationRepository;

    @Autowired
    protected BoardColumnSettingRepository boardColumnSettingRepository;

    @Autowired
    protected MessageRepository messageRepository;

    @Autowired
    protected ChatRepository chatRepository;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void clearDatabase() {
        issueActivityRepository.deleteAll();
        commentRepository.deleteAll();
        issueRepository.deleteAll();
        boardColumnSettingRepository.deleteAll();
        sprintRepository.deleteAll();
        invitationRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        subscriptionRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("password");
        return userRepository.save(user);
    }

    protected String bearerTokenFor(User user) {
        String token =
                jwtUtil.generateToken(
                        new UsernamePasswordAuthenticationToken(user.getEmail(), "password"));
        return "Bearer " + token;
    }

    protected Project createProjectViaService(User owner, String name, PROJECT_FRAMEWORK framework) {
        ProjectRequest request = new ProjectRequest();
        request.setName(name);
        request.setDescription(name + " description");
        request.setCategory("Engineering");
        request.setTags(List.of("teamboard"));
        request.setFramework(framework.name());
        Response<Project> created = projectService.createProject(request, owner);
        return created.getData();
    }

    protected void addMemberWithRole(Project project, User user, PROJECT_MEMBER_ROLE role) {
        if (project.getTeam().stream().noneMatch(existing -> existing.getId().equals(user.getId()))) {
            project.getTeam().add(user);
            projectRepository.save(project);
        }
        ProjectMembership membership = new ProjectMembership();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(role);
        projectMembershipRepository.save(membership);
    }

    protected Sprint createSprint(Project project, String name, SPRINT_STATUS status) {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setGoal("Sprint goal");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(7));
        sprint.setStatus(status);
        sprint.setProject(project);
        return sprintRepository.save(sprint);
    }

    protected Issue createIssue(
            Project project, User creator, User assignee, ISSUE_STATUS status, Sprint sprint) {
        Issue issue = new Issue();
        issue.setTitle("Issue " + System.nanoTime());
        issue.setDescription("Issue description");
        issue.setPriority(ISSUE_PRIORITY.MEDIUM);
        issue.setStatus(status);
        issue.setCreatedBy(creator);
        issue.setAssignedBy(creator);
        issue.setAssignee(assignee);
        issue.setAssignedDate(LocalDate.now());
        issue.setDueDate(LocalDate.now().plusDays(3));
        issue.setProject(project);
        issue.setSprint(sprint);
        return issueRepository.save(issue);
    }
}
