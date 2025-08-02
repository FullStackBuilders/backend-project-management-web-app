package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.User;

public interface UserService {


    User findByUsername(String email);

    User findByUserId(Long userId);

    User updateUserProjectSize(User user, int number);




}
