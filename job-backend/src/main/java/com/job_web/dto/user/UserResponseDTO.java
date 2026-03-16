package com.job_web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponseDTO {
    private long id;
    private String fullName;
    private String email;
    private String role;
    private LocalDate dateOfBirth;
    private String address;
    private String mobile;
    private boolean accountLocked;
    private boolean enabled;
    private boolean active;
    private boolean oauth2Enabled;
    private LocalDateTime createDate;
    private LocalDateTime lastModifiedDate;
}
