package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.PaymentResponse;
import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService{

    @Value("${secret.apiKey}")
    private String stripeSecretKey;

    @Override
    public PaymentResponse  createPaymentLink(PLAN_TYPE planType, long amount) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        SessionCreateParams params = SessionCreateParams.builder().addPaymentMethodType(
                SessionCreateParams.
                        PaymentMethodType.CARD).setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:3000/payment/success" + planType.name())
                        .setCancelUrl("http://localhost:3000/payment/fail")
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                 .setQuantity(1L).setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("USD")
                                        .setUnitAmount((long) amount)
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Subscription for " + planType.name())
                                                .build())
                                        .build()
                                ).build()
                        ).build();


        Session session = Session.create(params);
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPayment_url(session.getUrl());

        return paymentResponse;
    }
}
