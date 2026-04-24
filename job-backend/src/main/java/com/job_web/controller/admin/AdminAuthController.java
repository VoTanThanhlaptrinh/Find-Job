package com.job_web.controller.admin;

import com.job_web.dto.admin.auth.AdminLoginRequest;
import com.job_web.dto.admin.auth.AdminLoginResponse;
import com.job_web.dto.admin.auth.AdminLogoutRequest;
import com.job_web.dto.admin.auth.AdminRefreshRequest;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AuthService;
import com.job_web.service.admin.AdminService;
import com.job_web.utills.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "api/auth/admin", produces = "application/json")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request,
                                                     HttpServletResponse response) {
        String accessToken = authService.loginAdmin(loginDTO, request, response);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"),
                accessToken,
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request,
                                                                    HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.logout.success"), null, HttpStatus.OK.value()));
    }
}
