package com.job_web.dto.profile;

import com.job_web.models.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record UserInfo(
        @NotNull(message = "TÃªn Ä‘áº§y Ä‘á»§ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String fullName,

        @NotNull(message = "NgÃ y sinh khÃ´ng Ä‘Æ°á»£c rá»—ng")
        LocalDate dateOfBirth,

        @NotNull(message = "Äá»‹a chá»‰ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String address,

        @NotNull(message = "Sá»‘ Ä‘iá»‡n thoáº¡i khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String mobile
) {
    @AssertTrue(message = "khÃ´ng pháº£i Ä‘inh dáº¡ng sá»‘ Ä‘iá»‡n thoáº¡i")
    public boolean isMobile() {
        Pattern pattern = Pattern.compile("^\\d{10}$");
        Matcher matcher = pattern.matcher(mobile);
        return matcher.matches();
    }

    public static UserInfo fromUser(User userLogin) {
        return new UserInfo(
                userLogin.getFullName(),
                userLogin.getDateOfBirth(),
                userLogin.getAddress(),
                userLogin.getMobile()
        );
    }

    public void update(User userLogin) {
        userLogin.setFullName(fullName);
        userLogin.setDateOfBirth(dateOfBirth);
        userLogin.setAddress(address);
        userLogin.setMobile(mobile);
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
