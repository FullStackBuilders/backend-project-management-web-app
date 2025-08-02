package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatServiceImpl implements ChatService{

    private final ChatRepository chatRepository;
    private final ProjectService projectService;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, ProjectService projectService) {
        this.chatRepository = chatRepository;
        this.projectService = projectService;
    }


    @Override
    public Chat createChatForProject(Project project){
        Chat chat = new Chat();
        chat.setProject(project);
        return chatRepository.save(chat);
    }


    @Override
    public Response<Chat> getChatByProjectId(Long projectId) {
        Project project = projectService.getProjectById(projectId).getData();
        if (project.getChat() == null) {
            throw new ResourceNotFoundException("Chat not found for project with ID: " + projectId);

        }
        return Response.<Chat>builder()
                .data(project.getChat())
                .message("Chat retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
