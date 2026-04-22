package com.job_web.controller.admin;

import com.job_web.dto.admin.auth.AdminLoginRequest;
import com.job_web.dto.admin.auth.AdminLoginResponse;
import com.job_web.dto.admin.auth.AdminLogoutRequest;
import com.job_web.dto.admin.auth.AdminRefreshRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.admin.AdminService;
import com.job_web.utills.MessageUtils;
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
@RequestMapping(path = "/admin/auth", produces = "application/json")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody AdminLoginRequest request,
                                                     HttpServletResponse response) {
        String accessToken = adminService.login(request, response);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                accessToken, 
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> refresh(@RequestBody AdminRefreshRequest request) {
        AdminLoginResponse response = adminService.refresh(request);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                response, 
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout(@RequestBody AdminLogoutRequest request) {
        adminService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("loggedOut", true), 
                HttpStatus.OK.value()
        ));
    }
}
