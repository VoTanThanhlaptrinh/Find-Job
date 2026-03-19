package com.job_web.controller.account;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/auth", produces = "application/json")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody @Valid LoginDTO login,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = authService.login(login, request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegistationForm registationForm,
                                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = authService.register(registationForm);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @Async
    @GetMapping("/activation-links/{email}")
    public CompletableFuture<ResponseEntity<ApiResponse<String>>> sendActivationLink(@PathVariable String email) {
        ApiResponse<String> res = authService.sendLinkActivate(email);
        return CompletableFuture.completedFuture(ResponseEntity.status(res.getStatus()).body(res));
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<ApiResponse<String>> activateAccount(@PathVariable String token) {
        ApiResponse<String> res = authService.activeAccount(token);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/refreshToken")
    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        ApiResponse<String> res = authService.refreshToken(request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        ApiResponse<String> res = authService.logout(request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/google/url")
    public ResponseEntity<ApiResponse<String>> googleUrl(HttpServletRequest req) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String url = base + "/oauth2/authorization/google";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success", url, HttpStatus.OK.value()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> checkLogin(Principal principal) {
        ApiResponse<String> res = authService.checkLogin(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/password/code/{email}")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordCode(@PathVariable String email,
                                                                      HttpServletRequest request) {
        ApiResponse<String> res = authService.sendCodeForgotPassword(request, email);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPassDTO forgotPassDTO,
                                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = authService.forgotPassword(forgotPassDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/password/check-random/{random}")
    public ResponseEntity<ApiResponse<String>> checkRandom(@PathVariable String random) {
        ApiResponse<String> res = authService.checkRandom(random);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid ResetDTO resetDTO,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = authService.resetPassword(resetDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
