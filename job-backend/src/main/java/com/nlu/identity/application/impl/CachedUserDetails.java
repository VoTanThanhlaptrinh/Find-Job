package com.nlu.identity.application.impl;

import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.PhoneNumber;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Lightweight, password-free projection of {@link User} stored as JSON bytes in Redis.
 */
public record CachedUserDetails(
        long id,
        String fullName,
        LocalDate dateOfBirth,
        String role,
        String email,
        String address,
        String mobile,
        boolean accountLocked,
        boolean enabled,
        boolean active,
        boolean oauth2Enabled
) {
    public static CachedUserDetails fromUser(User user) {
        return new CachedUserDetails(
                user.getId(),
                user.getFullName(),
                user.getDateOfBirth(),
                user.getRole(),
                user.getEmail(),
                user.getAddress(),
                user.getMobile(),
                user.isAccountLocked(),
                user.isEnabled(),
                user.isActive(),
                user.isOauth2Enabled()
        );
    }

    public User toDetachedUser() {
        EmailAddress emailVo = (email != null && !email.isBlank()) ? new EmailAddress(email) : null;
        PhoneNumber phoneVo = (mobile != null && !mobile.isBlank()) ? new PhoneNumber(mobile) : null;
        return new User(
                id,
                fullName,
                null,
                dateOfBirth,
                role,
                emailVo,
                address,
                phoneVo,
                accountLocked,
                enabled,
                active,
                oauth2Enabled,
                new LinkedList<>(),
                new ArrayList<>()
        );
    }
}

