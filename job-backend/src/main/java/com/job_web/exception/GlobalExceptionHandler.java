package com.job_web.exception;

import com.job_web.utills.MessageUtils;
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
        String message = MessageUtils.getMessage(ex.getMessage());
        ApiResponse<String> response = new ApiResponse<>(message, null, ex.getStatus().value());
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
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = MessageUtils.getMessage("error.param_format", ex.getName());
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        String message = MessageUtils.getMessage("error.system", ex.getMessage());
        ApiResponse<String> response = new ApiResponse<>(message, null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
