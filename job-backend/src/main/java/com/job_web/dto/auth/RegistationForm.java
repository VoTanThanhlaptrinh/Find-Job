package com.job_web.dto.auth;

import com.job_web.constant.RoleConstants;
import com.job_web.custom.EmailExist;
import com.job_web.models.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

public record RegistationForm(
        @NotBlank(message = "{validation.fullname.required}")
        @Size(max = 255, message = "{validation.fullname.max}")
        String fullName,

        @NotBlank(message = "{validation.username.required}")
        @Email(message = "{validation.email}")
        @EmailExist
        String username,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, message = "{validation.password.min}")
        String password,

        @NotBlank(message = "{validation.confirm_password.required}")
        String confirmPassword
) {
    @AssertTrue(message = "{validation.password.mismatch}")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public User toUser(PasswordEncoder passwordEncoder, String role) {
        User user = new User();
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(username);
        user.setFullName(fullName);
        user.setAddress("");
        user.setRole(RoleConstants.normalizeRole(role));
        user.setMobile("");
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}
