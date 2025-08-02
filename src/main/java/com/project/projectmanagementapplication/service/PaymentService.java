package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.PaymentResponse;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;

public interface PaymentService {

    public PaymentResponse createPaymentLink(PLAN_TYPE planType, long amount) throws Exception;
}
