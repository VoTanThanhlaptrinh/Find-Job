package com.nlu.payment.application.impl;

import com.nlu.identity.domain.model.User;
import com.nlu.payment.api.dto.req.CreatePaymentRequest;
import com.nlu.payment.api.dto.res.PaymentResponse;
import com.nlu.payment.application.PaymentService;
import com.nlu.payment.domain.model.Payment;
import com.nlu.payment.domain.model.PaymentStatus;
import com.nlu.payment.domain.repository.PaymentRepository;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, User currentUser) {
        long totalStartNs = System.nanoTime();
        if (currentUser == null) {
            throw new UnauthorizedException("User authentication is required");
        }
        if (request == null) {
            throw new BadRequestException("Payment request is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than 0");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new BadRequestException("Currency is required");
        }
        if (request.paymentMethod() == null) {
            throw new BadRequestException("Payment method is required");
        }

        String transactionCode = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Payment payment = Payment.builder()
                .transactionCode(transactionCode)
                .userId(currentUser.getId())
                .amount(request.amount())
                .currency(request.currency().trim().toUpperCase())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.COMPLETED)
                .description(request.description() != null ? request.description().trim() : null)
                .build();
        long javaBuildMs = (System.nanoTime() - totalStartNs) / 1_000_000L;

        long dbStartNs = System.nanoTime();
        Payment saved = paymentRepository.save(payment);
        long dbSaveMs = (System.nanoTime() - dbStartNs) / 1_000_000L;

        long mapStartNs = System.nanoTime();
        PaymentResponse response = toResponse(saved);
        long javaMapMs = (System.nanoTime() - mapStartNs) / 1_000_000L;

        long totalMs = (System.nanoTime() - totalStartNs) / 1_000_000L;
        log.info("Created payment id={} transactionCode={} for userId={}", saved.getId(), saved.getTransactionCode(), currentUser.getId());
        log.info("[PERF][Service:DB][PaymentService] createPayment userId={} | total={}ms (javaValidateAndBuild={}ms, dbSave={}ms, javaMapResponse={}ms)",
                currentUser.getId(), totalMs, javaBuildMs, dbSaveMs, javaMapMs);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, User currentUser) {
        long totalStartNs = System.nanoTime();
        if (currentUser == null) {
            throw new UnauthorizedException("User authentication is required");
        }
        if (id == null || id <= 0) {
            throw new BadRequestException("Valid payment ID is required");
        }

        long dbStartNs = System.nanoTime();
        Payment payment = paymentRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        long dbQueryMs = (System.nanoTime() - dbStartNs) / 1_000_000L;

        long mapStartNs = System.nanoTime();
        PaymentResponse response = toResponse(payment);
        long javaMapMs = (System.nanoTime() - mapStartNs) / 1_000_000L;

        log.info("[PERF][Service:DB][PaymentService] getPaymentById id={} userId={} | total={}ms (dbQuery={}ms, javaMapResponse={}ms)",
                id, currentUser.getId(), (System.nanoTime() - totalStartNs) / 1_000_000L, dbQueryMs, javaMapMs);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(User currentUser) {
        long totalStartNs = System.nanoTime();
        if (currentUser == null) {
            throw new UnauthorizedException("User authentication is required");
        }

        long dbStartNs = System.nanoTime();
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        long dbQueryMs = (System.nanoTime() - dbStartNs) / 1_000_000L;

        long mapStartNs = System.nanoTime();
        List<PaymentResponse> responses = payments.stream()
                .map(this::toResponse)
                .toList();
        long javaMapMs = (System.nanoTime() - mapStartNs) / 1_000_000L;

        log.info("[PERF][Service:DB][PaymentService] getMyPayments userId={} count={} | total={}ms (dbQuery={}ms, javaMapResponse={}ms)",
                currentUser.getId(), responses.size(), (System.nanoTime() - totalStartNs) / 1_000_000L, dbQueryMs, javaMapMs);
        return responses;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionCode(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getDescription(),
                payment.getCreatedAt()
        );
    }
}

