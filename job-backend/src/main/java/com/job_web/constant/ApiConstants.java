package com.job_web.constant;

public class ApiConstants {

    // 1. Các API cho phép tất cả mọi người truy cập (Không phân biệt Method)
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",     // Bao gồm login, register, refresh-token, google, v.v.
            "/api/home/**",
            "/error",
            "/oauth2/**",
            "/login/oauth2/**",
            "/auth/**" ,        // Gộp chung /auth/.* và /auth/status
            "/api/jobs/filter"
    };

    // 2. Các API Public nhưng chỉ cho phép method GET
    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/jobs/**",
            "/api/blogs/**"
    };

    // 4. Các API yêu cầu quyền HIRER
    public static final String[] HIRER_ENDPOINTS = {
            "/api/hirer/**",
            "/api/account/roles/hirer"
    };

    // 5. Các API yêu cầu quyền USER
    public static final String[] USER_ENDPOINTS = {
            "/api/user/**",
            "/api/account/roles/user"
    };
}
