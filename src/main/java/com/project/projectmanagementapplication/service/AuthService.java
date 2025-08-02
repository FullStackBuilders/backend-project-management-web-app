package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.LoginRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.User;

public interface AuthService {

    Response<Void> registerUser(User userRegistrationRequest);

    Response<Void> authenticateUser( LoginRequest loginRequest);

}
