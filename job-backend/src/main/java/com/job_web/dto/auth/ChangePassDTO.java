package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePassDTO(
        @NotNull(message = "password hiá»‡n táº¡i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String oldPass,

        @NotNull(message = "password má»›i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(min = 8, message = "máº­t kháº©u tá»‘i thiá»ƒu 8 kÃ½ tá»±")
        String newPass,

        @NotNull(message = "password nháº­p láº¡i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String confirmPass
) {
    @AssertTrue(message = "password má»›i vÃ  password nháº­p láº¡i pháº£i khá»›p nhau")
    public boolean isPasswordsMatch() {
        return newPass != null && newPass.equals(confirmPass);
    }

    public String getOldPass() {
        return oldPass;
    }

    public String getNewPass() {
        return newPass;
    }

    public String getConfirmPass() {
        return confirmPass;
    }
}
