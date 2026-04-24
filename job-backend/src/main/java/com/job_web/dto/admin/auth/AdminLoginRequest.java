package com.job_web.dto.admin.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder

public record AdminLoginRequest(
        @NotBlank(message = "{validation.username.required}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}
