package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectMembershipService projectMembershipService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Test
    void createProject_frameworkNull_defaultsToKanban() {
        User owner = new User();
        owner.setId(1L);

        ProjectRequest request = new ProjectRequest();
        request.setName("P");
        request.setDescription("D");
        request.setCategory("Cat");
        request.setTags(List.of());
        request.setFramework(null);

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        Chat chat = new Chat();
        when(chatService.createChatForProject(any(Project.class))).thenReturn(chat);
        doNothing().when(projectMembershipService).createOwnerMembership(any(Project.class), eq(owner));

        projectService.createProject(request, owner);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertEquals(PROJECT_FRAMEWORK.KANBAN, captor.getValue().getFramework());
        verify(projectMembershipService).createOwnerMembership(any(Project.class), eq(owner));
    }

    @Test
    void createProject_frameworkScrum_setsScrum() {
        User owner = new User();
        owner.setId(1L);

        ProjectRequest request = new ProjectRequest();
        request.setName("P");
        request.setDescription("D");
        request.setCategory("Cat");
        request.setTags(List.of());
        request.setFramework("scrum");

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatService.createChatForProject(any(Project.class))).thenReturn(new Chat());
        doNothing().when(projectMembershipService).createOwnerMembership(any(Project.class), eq(owner));

        projectService.createProject(request, owner);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertEquals(PROJECT_FRAMEWORK.SCRUM, captor.getValue().getFramework());
        verify(projectMembershipService).createOwnerMembership(any(Project.class), eq(owner));
    }

    @Test
    void createProject_invalidFramework_throwsBadRequest() {
        User owner = new User();
        ProjectRequest request = new ProjectRequest();
        request.setName("P");
        request.setDescription("D");
        request.setCategory("Cat");
        request.setFramework("WATERFALL");

        assertThrows(BadRequestException.class, () -> projectService.createProject(request, owner));
        verify(projectRepository, never()).save(any());
        verify(chatService, never()).createChatForProject(any());
        verify(projectMembershipService, never()).createOwnerMembership(any(), any());
    }
}
