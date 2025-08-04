package com.project.projectmanagementapplication.service;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.mocks.MockProjectFactory;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.http.HttpStatus;

public class ProjectServiceTest {


    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ProjectRepository projectRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetProjectById_Success() {
        // Arrange
        Long projectId = 1L;
        Project mockProject = MockProjectFactory.createMockProject(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));

        // Act
        Response<Project> response = projectService.getProjectById(projectId);

        // Assert
        assertNotNull(response);
        assertEquals("Test project", response.getData().getName());
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("Project retrieved successfully", response.getMessage());
        assertNotNull(response.getTimestamp());

        verify(projectRepository, times(1)).findById(projectId);
    }

    @Test
    void testGetProjectById_NotFound() {
        // Arrange
        Long projectId = 99L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.getProjectById(projectId);
        });

        verify(projectRepository, times(1)).findById(projectId);
    }
}

