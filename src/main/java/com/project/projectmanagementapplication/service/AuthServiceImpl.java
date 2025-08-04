package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.LoginRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.UserRepository;
import com.project.projectmanagementapplication.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final SubscriptionService subscriptionService;

    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, SubscriptionService subscriptionService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.subscriptionService = subscriptionService;
        this.authenticationManager = authenticationManager;
    }


    @Override
    public Response<Void> registerUser(User userRegistrationRequest) {

        Optional<User> isUserExists = userRepository.findByEmail(userRegistrationRequest.getEmail());

        if (isUserExists.isPresent()) {
            throw new RuntimeException("User already exists with this email");
        }

        User createdUser = new User();

        createdUser.setPassword(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        createdUser.setEmail(userRegistrationRequest.getEmail());
        createdUser.setFirstName(userRegistrationRequest.getFirstName());
        createdUser.setLastName(userRegistrationRequest.getLastName());

        User savedUser = userRepository.save(createdUser);

        subscriptionService.createSubscription(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userRegistrationRequest.getEmail(),userRegistrationRequest.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateToken(authentication);

        return Response.<Void>builder().
                        message("Registration successful!").
                        status(HttpStatus.CREATED).
                        statusCode(HttpStatus.CREATED.value()).
                        accessToken(jwt).
                        timestamp(LocalDateTime.now().toString()).build();

    }

    @Override
    public Response<Void> authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        //Set context which is cleared after each request is served
        //This makes the behaviour stateless
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtil.generateToken(authentication);
        return Response.<Void>builder()
                .message("Login successful!")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .accessToken(jwt)
                .timestamp(LocalDateTime.now().toString())
                .build();


    }


}
