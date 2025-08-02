package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.exception.ResourceNotFoundException;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.UserRepository;
import com.project.projectmanagementapplication.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{


    private final UserRepository userRepository;


    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;

    }


    @Override
    public User findByUsername(String email){

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    }

    @Override
    public User findByUserId(Long userId){
         return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Override
    public User updateUserProjectSize(User user, int number) {
        user.setProjectSize(user.getProjectSize() + number);
        userRepository.save(user);
        return user;

    }
}
