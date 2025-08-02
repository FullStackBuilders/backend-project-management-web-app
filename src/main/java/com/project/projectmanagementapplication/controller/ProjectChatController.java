package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.ChatMessageRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.ChatMessage;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.MessageService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ProjectChatController {
    private final UserService userService;
    private final MessageService messageService;

    @Autowired
    public ProjectChatController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @PostMapping("project/{projectId}/send")
    public ResponseEntity<Response<ChatMessage>> sendMessage(@PathVariable Long projectId, @RequestBody ChatMessageRequest chatMessageRequest) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<ChatMessage> response = messageService.sendMessage(user.getId(), projectId, chatMessageRequest.getContent());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("project/{projectId}")
    public ResponseEntity<Response<List<ChatMessage>>> getMessagesByProjectId(@PathVariable Long projectId) throws Exception {
        Response<List<ChatMessage>> response = messageService.getMessagesByProjectId(projectId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
