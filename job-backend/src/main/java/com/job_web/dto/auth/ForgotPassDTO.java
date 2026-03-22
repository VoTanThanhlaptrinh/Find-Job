package com.job_web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record ForgotPassDTO(
        @NotNull(message = "email rá»—ng")
        String email,

        @NotNull(message = "mÃ£ xÃ¡c thá»±c rá»—ng")
        String code
) {
    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
