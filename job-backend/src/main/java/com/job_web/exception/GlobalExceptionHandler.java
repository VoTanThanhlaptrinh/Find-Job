package com.job_web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.job_web.dto.common.ApiResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException ex) {
        ApiResponse<String> response = new ApiResponse<>(ex.getMessage(), null, ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<String>> handleValidationException(Exception ex) {
        String message = "Validation error";
        if (ex instanceof MethodArgumentNotValidException e) {
            message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException e) {
            message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ApiResponse<String> response = new ApiResponse<>("Format tham số không đúng: " + ex.getName(), null, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        ApiResponse<String> response = new ApiResponse<>("Lỗi hệ thống không xác định: " + ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
