package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetDTO(
        @Size(min = 8, message = "Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c dÆ°á»›i 8 kÃ½ tá»±")
        @NotBlank(message = "Máº­t kháº©u má»›i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String newPass,

        @NotBlank(message = "XÃ¡c nháº­n máº­t kháº©u má»›i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String confirmPass,

        @NotBlank(message = "MÃ£ xÃ¡c nháº­n khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String random
) {
    @AssertTrue(message = "Máº­t kháº©u má»›i vÃ  máº­t kháº©u xÃ¡c nháº­n khÃ´ng giá»‘ng nhau")
    public boolean isValid() {
        return newPass.equals(confirmPass);
    }

    public String getNewPass() {
        return newPass;
    }

    public String getConfirmPass() {
        return confirmPass;
    }

    public String getRandom() {
        return random;
    }
}
