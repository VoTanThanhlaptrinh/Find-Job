package com.job_web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCrudDTO(
        @NotBlank(message = "{validation.fullname.required}")
        @Size(max = 255, message = "{validation.fullname.max}")
        String fullName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, message = "{validation.password.min}")
        String password,

        @NotBlank(message = "{validation.role.required}")
        String role,

        @NotNull(message = "{validation.dob.required}")
        @Past(message = "{validation.dob.past}")
        LocalDate dateOfBirth,

        @NotBlank(message = "{validation.address.required}")
        String address,

        @NotBlank(message = "{validation.phone.required}")
        @Pattern(regexp = "^\\d{10}$", message = "{validation.phone.invalid}")
        String mobile,

        @NotNull(message = "{validation.active.required}")
        Boolean active,

        @NotNull(message = "{validation.locked.required}")
        Boolean accountLocked,

        @NotNull(message = "{validation.enabled.required}")
        Boolean enabled,

        @NotNull(message = "{validation.oauth2.required}")
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
