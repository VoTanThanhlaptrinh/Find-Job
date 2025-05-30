package com.job_web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.job_web.dto.ApiResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<String>> handlerNoSuchElement(ResourceNotFoundException ex) {
		ApiResponse<String> response = new ApiResponse<String>(ex.getMessage(), null, HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(response);
	}
}
