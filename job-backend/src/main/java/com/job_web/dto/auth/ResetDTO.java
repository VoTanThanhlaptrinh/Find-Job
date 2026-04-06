package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetDTO(
        @Size(min = 8, message = "{validation.password.min}")
        @NotBlank(message = "{validation.new_password.required}")
        String newPass,

        @NotBlank(message = "{validation.confirm_password.required}")
        String confirmPass,

        @NotBlank(message = "{validation.verification_code.required}")
        String random
) {
    @AssertTrue(message = "{validation.new_password.mismatch}")
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
