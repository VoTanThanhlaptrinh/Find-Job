package com.job_web.dto.auth;

import com.job_web.custom.EmailExist;
import com.job_web.models.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

public record RegistationForm(
        @NotBlank(message = "Báº¡n pháº£i chá»n vai trÃ² cá»§a tÃ i khoáº£n Ä‘Äƒng nháº­p")
        String role,

        @NotBlank(message = "TÃªn Ä‘áº§y Ä‘á»§ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(max = 255, message = "TÃªn Ä‘áº§y Ä‘á»§ chá»‰ Ä‘Æ°á»£c tá»‘i Ä‘a 255 kÃ½ tá»±")
        String fullName,

        @NotBlank(message = "TÃªn tÃ i khoáº£n khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Email(message = "KhÃ´ng pháº£i Email")
        @EmailExist
        String username,

        @NotBlank(message = "Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(min = 8, message = "Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c dÆ°á»›i 8 kÃ½ tá»±")
        String password,

        @NotBlank(message = "xÃ¡c nháº­n máº­t kháº©u khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String confirmPassword
) {
    @AssertTrue(message = "Máº­t kháº©u vÃ  xÃ¡c nháº­n máº­t kháº©u khÃ´ng khá»›p")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public User toUser(PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(username);
        user.setFullName(fullName);
        user.setAddress("");
        user.setMobile("");
        user.setRole(role);
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }

    public String getRole() {
        return role;
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
