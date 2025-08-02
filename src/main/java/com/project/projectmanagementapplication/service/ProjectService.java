package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;

import java.util.List;

public interface ProjectService {

    Response<Project> createProject(ProjectRequest projectRequest, User user) ;

    Response<List<Project>> getAllProjectForUser(User user, String category, String tag);

    Response<Project> getProjectById(Long projectId) ;

    Response<Void> deleteProject(Long projectId, Long userId);

    Response<Project> updateProject(ProjectRequest projectRequest, Long id);

    Response<Void> addUserToProject( Long userId ,Long projectId) ;

    Response<Void> removeUserFromProject(Long userId, Long projectId) ;

    Response<List<Project>> searchProjects(String keyword, User user) ;
}
