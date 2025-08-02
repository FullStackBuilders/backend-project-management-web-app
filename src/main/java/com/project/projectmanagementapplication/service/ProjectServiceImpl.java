package com.project.projectmanagementapplication.service;


import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.exception.ConflictException;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ChatService chatService;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository, UserService userService, @Lazy ChatService chatService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.chatService = chatService;
    }


    @Override
    public Response<Project> createProject(ProjectRequest projectRequest, User user) {
        Project project = new Project();
        project.setOwner(user);
        project.setName(projectRequest.getName());
        project.setCategory(projectRequest.getCategory());
        project.setDescription(projectRequest.getDescription());
        project.setTags(projectRequest.getTags());
        project.getTeam().add(user);
        Project savedProject = projectRepository.save(project);
        Chat savedChat = chatService.createChatForProject(savedProject);
        savedProject.setChat(savedChat);

        return Response.<Project>builder()
                .message("Project created successfully")
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now().toString())
                .data(savedProject)
                .build();
    }

    @Override
    public Response<List<Project>> getAllProjectForUser(User user, String category, String tag){
        List<Project> projects = projectRepository.findByTeamContainingOrOwner(user, user);
        if (category != null){
            projects = projects.stream().filter(p -> p.getCategory().equalsIgnoreCase(category))
                       .collect(Collectors.toList());
        }
        if (tag != null) {
            projects = projects.stream().filter(p -> p.getTags().contains(tag))
                       .collect(Collectors.toList());

        }
        return Response.<List<Project>>builder()
                .message("Projects retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .data(projects)
                .build();
    }

    @Override
    public Response<Project> getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));
        return Response.<Project>builder()
                .message("Project retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .data(project)
                .build();

    }

        @Override
        public Response<Void> deleteProject(Long projectId, Long userId){
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() ->new ResourceNotFoundException("Project not found with ID: " + projectId));


            if (!project.getOwner().getId().equals(userId)) {
                throw new UnauthorizedException("You are not authorized to delete this project");
            }

            projectRepository.delete(project);

            return Response.<Void>builder()
                    .message("Project deleted successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }


    @Override
    public Response<Project> updateProject(ProjectRequest projectRequest, Long projectId){
        Project project = getProjectById(projectId).getData();

        // Apply updates only if values are provided
        if (projectRequest.getName() != null) {
            project.setName(projectRequest.getName());
        }

        if (projectRequest.getDescription() != null) {
            project.setDescription(projectRequest.getDescription());
        }

        if (projectRequest.getCategory() != null) {
            project.setCategory(projectRequest.getCategory());
        }

        if (projectRequest.getTags() != null) {
            project.setTags(projectRequest.getTags());
        }

        Project updatedProject = projectRepository.save(project);

        return Response.<Project>builder()
                .message("Project updated successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .data(updatedProject)
                .build();
    }


    @Override
    public Response<Void> addUserToProject(Long userId, Long projectId){
        User user = userService.findByUserId(userId);
        Project project = getProjectById(projectId).getData();

        if(!(project.getTeam().contains(user))) {
            project.getChat().getUsers().add(user);
            project.getTeam().add(user);
            projectRepository.save(project);
            userService.updateUserProjectSize(user, 1);
            return Response.<Void>builder()
                    .message("User added to project successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }
        throw new ConflictException("User already exists in the project");

    }

    @Override
    public Response<Void> removeUserFromProject(Long userId, Long projectId)  {
        Project project  = getProjectById(projectId).getData();
        User user = userService.findByUserId(userId);
        if(!(project.getTeam().contains(user))) {
            throw new ConflictException("User does not exist in the project");
        }
        project.getChat().getUsers().remove(user);
        project.getTeam().remove(user);
        projectRepository.save(project);
        userService.updateUserProjectSize(user, -1);
        return Response.<Void>builder()
                .message("User removed from project successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

    }


    @Override
    public Response<List<Project>> searchProjects(String keyword, User user) {
        List<Project> projects = projectRepository.findByNameContainingIgnoreCaseAndTeamContains(keyword, user);
        return Response.<List<Project>>builder()
                .message("Projects found matching keyword: " + keyword)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .data(projects)
                .build();}
 }
