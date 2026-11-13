package com.nlu.payment.api.dto.res;

import com.nlu.payment.domain.model.PaymentMethod;
import com.nlu.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String transactionCode,
        Long userId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String description,
        LocalDateTime createdAt
) {
}

