package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.ChatMessage;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.MessageRepository;
import com.project.projectmanagementapplication.repository.ProjectRepository;
import com.project.projectmanagementapplication.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

     private final MessageRepository messageRepository;
     private final UserService userService;
     private final ChatService chatService;

    public MessageServiceImpl(MessageRepository messageRepository, UserService userService, ChatService chatService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.chatService = chatService;
    }

    @Override
    public Response<ChatMessage> sendMessage(Long senderId, Long projectId, String content) throws Exception {
        User sender = userService.findByUserId(senderId);
        Chat chat = chatService.getChatByProjectId(projectId).getData();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setChat(chat);
        chatMessage.setContent(content);
        chatMessage.setCreatedAt(LocalDateTime.now());
        ChatMessage savedChatMessage = messageRepository.save(chatMessage);
        return Response.<ChatMessage>builder()
                .data(savedChatMessage)
                .message("Message sent successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();


    }

    @Override
    public Response<List<ChatMessage>>  getMessagesByProjectId(Long projectId) throws Exception {
        Chat chat = chatService.getChatByProjectId(projectId).getData();
        List<ChatMessage> chatMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId());
        return Response.<List<ChatMessage>>builder()
                .data(chatMessages)
                .message("Messages retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
