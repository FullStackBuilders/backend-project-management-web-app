package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import com.project.projectmanagementapplication.model.Subscription;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SubscriptionServiceImpl implements SubscriptionService{

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;

    }


    @Override
    public Response<Subscription> createSubscription(User user) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setSubscriptionStartDate(LocalDate.now());
        subscription.setSubscriptionEndDate(LocalDate.now().plusMonths(12));
        subscription.setPlanType(PLAN_TYPE.Free);
        subscription.setValid(true);
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return Response.<Subscription>builder()
                .data(savedSubscription)
                .message("Subscription created successfully")
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDate.now().toString())
                .build();
    }

    @Override
    public Response<Subscription> getSubscriptionByUserId(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId);
        if (!isValid(subscription)) {
            subscription.setPlanType(PLAN_TYPE.Free);
            subscription.setSubscriptionEndDate(LocalDate.now().plusMonths(12));
            subscription.setSubscriptionStartDate(LocalDate.now());
        }
        subscriptionRepository.save(subscription);

        return Response.<Subscription>builder()
                .data(subscription)
                .message("Subscription retrieved successfully")
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDate.now().toString())
                .build();
    }



    @Override
    public Response<Subscription> upgradeSubscription(Long userId, PLAN_TYPE planType) throws Exception {
        Subscription subscription = getSubscriptionByUserId(userId).getData();
        subscription.setPlanType(planType);
        subscription.setSubscriptionStartDate(LocalDate.now());
        if (planType.equals(PLAN_TYPE.Monthly)) {
            subscription.setSubscriptionEndDate(LocalDate.now().plusMonths(1));
        }
        if (planType.equals(PLAN_TYPE.Annual)) {
            subscription.setSubscriptionEndDate(LocalDate.now().plusMonths(12));
        }
            Subscription updatedSubscription = subscriptionRepository.save(subscription);

            return Response.<Subscription>builder()
                    .data(updatedSubscription)
                    .message("Subscription upgraded successfully to " + planType)
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(LocalDate.now().toString())
                    .build();
        }

        @Override
        public boolean isValid(Subscription subscription) {

            if (subscription.getPlanType().equals(PLAN_TYPE.Free)) {
                return true;
            }
            return subscription.getSubscriptionEndDate().isAfter(LocalDate.now()) ||
                    subscription.getSubscriptionEndDate().isEqual(LocalDate.now());

        }


}
