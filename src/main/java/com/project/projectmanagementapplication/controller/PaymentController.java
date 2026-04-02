package com.project.projectmanagementapplication.controller;


import com.project.projectmanagementapplication.dto.PaymentResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import com.project.projectmanagementapplication.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{planType}")
    public ResponseEntity<Response<PaymentResponse>> createPaymentLink(@PathVariable PLAN_TYPE planType) throws Exception {
        long amount = 799 * 100;
        if (planType.equals(PLAN_TYPE.Annual)) {
            amount = 1999 * 100;
        }

        PaymentResponse paymentResponse = paymentService.createPaymentLink(planType, amount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Response.<PaymentResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Payment link created successfully")
                        .data(paymentResponse)
                        .build());
    }

}
