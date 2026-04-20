package com.job_web.dto.admin.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginRequest {
    private String email;
    private String password;
    private boolean rememberMe;
}
