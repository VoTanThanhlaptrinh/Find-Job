package com.nlu.payment.api.dto.req;

import com.nlu.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}

