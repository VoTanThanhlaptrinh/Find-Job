package com.nlu.admin.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;


@Builder

public record AdminLoginRequest(
        @NotBlank(message = "{validation.username.required}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}
