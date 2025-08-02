package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;

public interface ChatService {
    Chat createChatForProject(Project project);

    Response<Chat> getChatByProjectId(Long projectId);
}
