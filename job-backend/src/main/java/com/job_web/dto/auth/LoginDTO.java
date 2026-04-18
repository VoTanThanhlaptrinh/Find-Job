package com.job_web.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(

        @NotBlank(message = "{validation.username.required}")
        String username,

        @NotBlank(message = "{validation.password.required}")
        String password
) {

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
