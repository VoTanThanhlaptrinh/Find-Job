package com.nlu.identity.domain.vo;

public class ApiConstants {

    // 1. Các API cho phép tất cả mọi người truy cập (Không phân biệt Method)
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",     // Bao gồm login, register, refresh-token, google, v.v.
            "/api/home/**",
            "/error",
            "/oauth2/**",
            "/login/oauth2/**",
            "/auth/**" ,
            "/api/jobs/filter",
            "/api/admin/auth/**",
    };

    // 2. Các API Public nhưng chỉ cho phép method GET
    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/jobs/**",
            "/api/blogs/**",
            "/api/*/categories/**",
            "/api/addresses/address-count"
    };

    // 3. Các API yêu cầu đăng nhập (bất kỳ role nào)
    public static final String[] AUTHENTICATED_ENDPOINTS = {
            "/api/account/profile",
            "/api/account/password",
            "/api/account/oauth2",
            "/api/sse/**",
            "/api/blogs"            // POST blog
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

    // 6. Cac API yeu cau quyen ADMIN
    public static final String[] ADMIN_ENDPOINTS = {
            "/api/admin/dashboard/**",
            "/api/admin/employers/**",
            "/api/admin/job-seekers/**",
            "/api/admin/jobs/**",
            "/api/admin/billing/**"
    };
}
