package com.job_web.dto.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponseDTO(
        long id,
        String fullName,
        String email,
        String role,
        LocalDate dateOfBirth,
        String address,
        String mobile,
        boolean accountLocked,
        boolean enabled,
        boolean active,
        boolean oauth2Enabled,
        LocalDateTime createDate,
        LocalDateTime lastModifiedDate
) {
    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
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

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isOauth2Enabled() {
        return oauth2Enabled;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
}
