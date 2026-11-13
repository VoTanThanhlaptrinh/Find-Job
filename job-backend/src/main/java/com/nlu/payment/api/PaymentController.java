package com.nlu.payment.api;

import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.payment.api.dto.req.CreatePaymentRequest;
import com.nlu.payment.api.dto.res.PaymentResponse;
import com.nlu.payment.application.PaymentService;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/payments", produces = "application/json")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @CurrentUser User currentUser) {
        long startNs = System.nanoTime();
        PaymentResponse response = paymentService.createPayment(request, currentUser);
        long serviceMs = (System.nanoTime() - startNs) / 1_000_000L;
        ResponseEntity<ApiResponse<PaymentResponse>> entity = ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(MessageUtils.getMessage("message.success"), response, HttpStatus.CREATED.value()));
        log.info("[PERF][Controller:Payment] createPayment | total={}ms (service={}ms)",
                (System.nanoTime() - startNs) / 1_000_000L, serviceMs);
        return entity;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long id,
            @CurrentUser User currentUser) {
        long startNs = System.nanoTime();
        PaymentResponse response = paymentService.getPaymentById(id, currentUser);
        long serviceMs = (System.nanoTime() - startNs) / 1_000_000L;
        ResponseEntity<ApiResponse<PaymentResponse>> entity = ResponseEntity.ok(
                new ApiResponse<>(MessageUtils.getMessage("message.success"), response, HttpStatus.OK.value()));
        log.info("[PERF][Controller:Payment] getPaymentById id={} | total={}ms (service={}ms)",
                id, (System.nanoTime() - startNs) / 1_000_000L, serviceMs);
        return entity;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @CurrentUser User currentUser) {
        long startNs = System.nanoTime();
        List<PaymentResponse> responses = paymentService.getMyPayments(currentUser);
        long serviceMs = (System.nanoTime() - startNs) / 1_000_000L;
        ResponseEntity<ApiResponse<List<PaymentResponse>>> entity = ResponseEntity.ok(
                new ApiResponse<>(MessageUtils.getMessage("message.success"), responses, HttpStatus.OK.value()));
        log.info("[PERF][Controller:Payment] getMyPayments count={} | total={}ms (service={}ms)",
                responses.size(), (System.nanoTime() - startNs) / 1_000_000L, serviceMs);
        return entity;
    }
}
