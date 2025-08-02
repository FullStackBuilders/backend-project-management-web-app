package com.project.projectmanagementapplication.controller;


import com.project.projectmanagementapplication.dto.LoginRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController {

   private final AuthService authService;

   @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Response<Void>> registerUserHandler(@RequestBody User user){
        Response<Void> response = authService.registerUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Response<Void>> loginUserHandler(@RequestBody LoginRequest loginRequest){
        Response<Void> response = authService.authenticateUser(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
