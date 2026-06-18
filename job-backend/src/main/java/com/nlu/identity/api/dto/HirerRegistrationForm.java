package com.nlu.identity.api.dto;

import com.nlu.identity.infrastructure.validation.EmailExist;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record HirerRegistrationForm(
        @NotBlank(message = "{validation.fullname.required}")
        @Size(max = 255, message = "{validation.fullname.max}")
        String fullName,

        @NotBlank(message = "{validation.company_name.required}")
        @Size(max = 255, message = "{validation.company_name.max}")
        String companyName,

        @NotBlank(message = "{validation.phone.required}")
        @Pattern(regexp = "^0[0-9]{9,10}$", message = "{validation.phone.invalid}")
        String phone,

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

    public String getFullName() {
        return fullName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getPhone() {
        return phone;
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
