package com.job_web.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePassDTO(
        @NotNull(message = "{validation.old_password.required}")
        String oldPass,

        @NotNull(message = "{validation.new_password.required}")
        @Size(min = 8, message = "{validation.password.min}")
        String newPass,

        @NotNull(message = "{validation.confirm_password.required}")
        String confirmPass
) {
    @AssertTrue(message = "{validation.new_password.mismatch}")
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
