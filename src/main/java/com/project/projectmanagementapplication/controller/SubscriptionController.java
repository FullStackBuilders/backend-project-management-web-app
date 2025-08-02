package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import com.project.projectmanagementapplication.model.Subscription;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.SubscriptionService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {


    private final SubscriptionService subscriptionService;
    private final UserService userService;


    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService, UserService userService) {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
    }

    @GetMapping("/user")
    public ResponseEntity<Response<Subscription>>getSubscriptionByUserId() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Subscription> response = subscriptionService.getSubscriptionByUserId(user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PatchMapping("/upgrade")
    public ResponseEntity<Response<Subscription>> upgradeSubscription(@RequestParam PLAN_TYPE planType) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Subscription> response = subscriptionService.upgradeSubscription(user.getId(), planType);
        return ResponseEntity.status(response.getStatus()).body(response);
    }


}
