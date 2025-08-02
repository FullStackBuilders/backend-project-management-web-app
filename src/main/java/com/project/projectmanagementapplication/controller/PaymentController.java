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
    public ResponseEntity<Response<PaymentResponse>>createPaymentLink(@PathVariable PLAN_TYPE planType) {
            try {
                // Assuming the planType is used to determine the amount or other parameters
                long amount = 799 * 100; // Example amount in cents (799.00)
                if(planType.equals(PLAN_TYPE.Annual)){
                    amount =  1999 * 100; // Annual plan, multiply by 12
                    //amount = (long) (amount * 0.9); // Apply 10% discount for annual plan
                }


                PaymentResponse paymentResponse = paymentService.createPaymentLink(planType,amount);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Response.<PaymentResponse>builder()
                                .status(HttpStatus.CREATED)
                                .message("Payment link created successfully")
                                .data(paymentResponse)
                                .build());
            } catch (Exception e) {
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.<PaymentResponse>builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Failed to create payment  link: " + e.getMessage())
                        .build());
            }
    }

}
