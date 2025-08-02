package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import com.project.projectmanagementapplication.model.Subscription;
import com.project.projectmanagementapplication.model.User;

public interface SubscriptionService {

    Response<Subscription> createSubscription(User user);

    Response<Subscription> getSubscriptionByUserId(Long userId) ;

    Response<Subscription> upgradeSubscription(Long userId, PLAN_TYPE planType) throws Exception;

    boolean isValid(Subscription subscription) throws Exception;
}
