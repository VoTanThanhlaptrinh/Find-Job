package com.nlu.admin.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private AdminInfo admin;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminInfo {
        private String id;
        private String fullName;
        private String email;
        private String role;
        private LocalDateTime lastLoginAt;
    }
}
