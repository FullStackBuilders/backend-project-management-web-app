package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.mapper.IssueMapper;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @InjectMocks
    private IssueServiceImpl issueService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Test
    void updateIssueStatus_setsLastEditedByAndLastEditedAt() throws Exception {
        Long issueId = 10L;
        Long assigneeUserId = 2L;

        User assignee = new User();
        assignee.setId(assigneeUserId);
        assignee.setFirstName("Alex");
        assignee.setLastName("Assignee");

        User owner = new User();
        owner.setId(100L);
        User creator = new User();
        creator.setId(50L);

        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(assignee);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(assigneeUserId)).thenReturn(assignee);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        Response<IssueResponse> response =
                issueService.updateIssueStatus(issueId, ISSUE_STATUS.IN_PROGRESS.toString(), assigneeUserId);

        assertNotNull(response);
        assertNotNull(response.getData());

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        Issue saved = savedCaptor.getValue();

        assertSame(assignee, saved.getLastEditedBy());
        assertNotNull(saved.getLastEditedAt());
        assertEquals(ISSUE_STATUS.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void removeAssigneeFromIssue_clearsAssigneeAndSetsAudit() throws Exception {
        Long issueId = 11L;
        Long ownerId = 100L;

        User owner = new User();
        owner.setId(ownerId);
        User assignee = new User();
        assignee.setId(2L);
        User creator = new User();
        creator.setId(ownerId);

        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTitle("Task");
        issue.setStatus(ISSUE_STATUS.TO_DO);
        issue.setProject(project);
        issue.setCreatedBy(creator);
        issue.setAssignee(assignee);
        issue.setAssignedBy(owner);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userService.findByUserId(ownerId)).thenReturn(owner);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueMapper.toIssueResponse(any(Issue.class), any(Project.class)))
                .thenReturn(IssueResponse.builder().id(issueId).build());

        Response<IssueResponse> response = issueService.removeAssigneeFromIssue(issueId, ownerId);

        assertNotNull(response.getData());

        ArgumentCaptor<Issue> savedCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(savedCaptor.capture());
        Issue saved = savedCaptor.getValue();

        assertNull(saved.getAssignee());
        assertSame(owner, saved.getAssignedBy());
        assertSame(owner, saved.getLastEditedBy());
        assertNotNull(saved.getLastEditedAt());
    }
}
