package com.job_web.exception;

import com.job_web.dto.common.ApiResponse;
import com.job_web.utills.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MDC_TRACE_ID_KEY = "traceId";


    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException ex) {

        log.warn("Business exception — key: {}, status: {}", ex.getMessage(), ex.getStatus().value());

        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        String message = MessageUtils.getMessage(ex.getMessage());
        ApiResponse<String> response = new ApiResponse<>(message, null, ex.getStatus().value(), traceId);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<String>> handleValidationException(Exception ex) {
        String message = MessageUtils.getMessage("error.validation");

        if (ex instanceof MethodArgumentNotValidException e) {
            message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException e) {
            message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        // WARN — no stack trace. Client sent invalid input — not a system error.
        log.warn("Validation failed — message: {}", message);

        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.BAD_REQUEST.value(), traceId);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        // WARN — no stack trace. Log only the parameter name, not the invalid value
        // (which could contain injected payloads or PII).
        log.warn("Type mismatch — param: {}, expectedType: {}",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        String message = MessageUtils.getMessage("error.param_format", ex.getName());
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.BAD_REQUEST.value(), traceId);

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);

        log.error("Unhandled exception [traceId={}]: ", traceId, ex);
        String message = MessageUtils.getMessage("error.system.internal");
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.INTERNAL_SERVER_ERROR.value(), traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}