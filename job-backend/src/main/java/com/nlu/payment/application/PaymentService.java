package com.nlu.payment.application;

import com.nlu.identity.domain.model.User;
import com.nlu.payment.api.dto.req.CreatePaymentRequest;
import com.nlu.payment.api.dto.res.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request, User currentUser);

    PaymentResponse getPaymentById(Long id, User currentUser);

    List<PaymentResponse> getMyPayments(User currentUser);
}

