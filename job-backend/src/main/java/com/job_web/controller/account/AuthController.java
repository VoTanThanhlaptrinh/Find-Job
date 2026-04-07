package com.job_web.controller.account;

import java.util.concurrent.CompletableFuture;

import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.service.account.AuthService;
import com.job_web.utills.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/auth", produces = "application/json")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Async
    @GetMapping("/activation-links/{email}")
    public CompletableFuture<ResponseEntity<ApiResponse<String>>> sendActivationLink(@PathVariable String email) {
        authService.sendLinkActivate(email);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.email.sent"), null, HttpStatus.OK.value())));
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<ApiResponse<String>> activateAccount(@PathVariable String token) {
        authService.activeAccount(token);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.activate.success"), null, HttpStatus.OK.value()));
    }

    @GetMapping("/refreshToken")
    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = authService.refreshToken(request, response);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), accessToken, HttpStatus.OK.value()));
    }

    @GetMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.logout.success"), null, HttpStatus.OK.value()));
    }

    @GetMapping("/google/url")
    public ResponseEntity<ApiResponse<String>> googleUrl(HttpServletRequest req) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String url = base + "/oauth2/authorization/google";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(MessageUtils.getMessage("message.success"), url, HttpStatus.OK.value()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> checkLogin(@CurrentUser User currentUser) {
        String name = authService.checkLogin(currentUser);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), name, HttpStatus.OK.value()));
    }

    @GetMapping("/password/code/{email}")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordCode(@PathVariable String email,
                                                                      HttpServletRequest request) {
        authService.sendCodeForgotPassword(request, email);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.password.code_sent"), null, HttpStatus.OK.value()));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPassDTO forgotPassDTO) {
        String random = authService.forgotPassword(forgotPassDTO);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), random, HttpStatus.OK.value()));
    }

    @GetMapping("/password/check-random/{random}")
    public ResponseEntity<ApiResponse<String>> checkRandom(@PathVariable String random) {
        authService.checkRandom(random);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid ResetDTO resetDTO) {
        authService.resetPassword(resetDTO);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));
    }
}
