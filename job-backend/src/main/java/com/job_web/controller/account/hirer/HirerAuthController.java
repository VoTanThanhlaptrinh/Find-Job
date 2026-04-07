package com.job_web.controller.account.hirer;

import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AuthService;
import com.job_web.utills.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth/hirer", produces = "application/json")
@RequiredArgsConstructor
public class HirerAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody @Valid LoginDTO login,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String accessToken = authService.loginHirer(login, request, response);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.login.success"), accessToken, HttpStatus.OK.value()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegistationForm registationForm) {
        String username = authService.registerHirer(registationForm);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.register.success"), username, HttpStatus.OK.value()));
    }
}
