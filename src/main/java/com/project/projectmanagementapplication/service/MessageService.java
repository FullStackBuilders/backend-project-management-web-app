package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.ChatMessage;

import java.util.List;

public interface MessageService {

    Response<ChatMessage> sendMessage(Long senderId, Long projectId, String Content   ) throws Exception;

    Response<List<ChatMessage>> getMessagesByProjectId(Long projectId) throws Exception;
}
