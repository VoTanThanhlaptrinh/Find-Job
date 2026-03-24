package com.job_web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCrudDTO(
        @NotBlank(message = "TÃªn Ä‘áº§y Ä‘á»§ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(max = 255, message = "TÃªn Ä‘áº§y Ä‘á»§ tá»‘i Ä‘a 255 kÃ½ tá»±")
        String fullName,

        @NotBlank(message = "Email khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Email(message = "KhÃ´ng pháº£i Email")
        String email,

        @NotBlank(message = "Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(min = 8, message = "Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c dÆ°á»›i 8 kÃ½ tá»±")
        String password,

        @NotBlank(message = "Vai trÃ² khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String role,

        @NotNull(message = "NgÃ y sinh khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Past(message = "NgÃ y sinh pháº£i trÆ°á»›c hÃ´m nay")
        LocalDate dateOfBirth,

        @NotBlank(message = "Äá»‹a chá»‰ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String address,

        @NotBlank(message = "Sá»‘ Ä‘iá»‡n thoáº¡i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Pattern(regexp = "^\\d{10}$", message = "KhÃ´ng pháº£i Ä‘á»‹nh dáº¡ng sá»‘ Ä‘iá»‡n thoáº¡i")
        String mobile,

        @NotNull(message = "Tráº¡ng thÃ¡i kÃ­ch hoáº¡t khÃ´ng Ä‘Æ°á»£c rá»—ng")
        Boolean active,

        @NotNull(message = "Tráº¡ng thÃ¡i khÃ³a tÃ i khoáº£n khÃ´ng Ä‘Æ°á»£c rá»—ng")
        Boolean accountLocked,

        @NotNull(message = "Tráº¡ng thÃ¡i enable khÃ´ng Ä‘Æ°á»£c rá»—ng")
        Boolean enabled,

        @NotNull(message = "Tráº¡ng thÃ¡i OAuth2 khÃ´ng Ä‘Æ°á»£c rá»—ng")
        Boolean oauth2Enabled
) {
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getMobile() {
        return mobile;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getAccountLocked() {
        return accountLocked;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getOauth2Enabled() {
        return oauth2Enabled;
    }
}
