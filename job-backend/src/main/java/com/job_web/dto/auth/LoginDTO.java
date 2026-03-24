package com.job_web.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "Báº¡n pháº£i chá»n vai trÃ² cá»§a tÃ i khoáº£n Ä‘Äƒng nháº­p")
        String role,

        @NotBlank(message = "username khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String username,

        @NotBlank(message = "password khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String password
) {
    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
