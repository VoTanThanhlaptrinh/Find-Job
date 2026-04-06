package com.job_web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record ForgotPassDTO(
        @NotNull(message = "{validation.email.required}")
        String email,

        @NotNull(message = "{validation.code.required}")
        String code
) {
    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
