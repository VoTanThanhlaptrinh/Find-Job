package com.job_web.dto.profile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record UserInfo(
        @NotNull(message = "{validation.fullname.required}")
        String fullName,

        @NotNull(message = "{validation.dob.required}")
        LocalDate dateOfBirth,

        @NotNull(message = "{validation.address.required}")
        String address,

        @NotNull(message = "{validation.phone.required}")
        String mobile
) {
    @AssertTrue(message = "{validation.phone.invalid}")
    public boolean isMobile() {
        Pattern pattern = Pattern.compile("^\\d{10}$");
        Matcher matcher = pattern.matcher(mobile);
        return matcher.matches();
    }



    public String getFullName() {
        return fullName;
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
}
