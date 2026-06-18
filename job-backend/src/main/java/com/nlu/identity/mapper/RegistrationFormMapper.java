package com.nlu.identity.mapper;

import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.domain.vo.RoleConstants;
import com.nlu.identity.api.dto.RegistrationForm;
import com.nlu.identity.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RegistrationFormMapper {

    public User toUser(RegistrationForm registrationForm, PasswordEncoder passwordEncoder, String role) {
        User user = new User();
        user.setPassword(new Password(passwordEncoder.encode(registrationForm.password())));
        user.setEmail(new EmailAddress(registrationForm.username()));
        user.setFullName(registrationForm.fullName());
        user.setAddress("");
        user.setRole(RoleConstants.normalizeRole(role));
        user.setMobile(null);
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }

    public User toUser(com.nlu.identity.api.dto.HirerRegistrationForm registrationForm, PasswordEncoder passwordEncoder, String role) {
        User user = new User();
        user.setPassword(new Password(passwordEncoder.encode(registrationForm.password())));
        user.setEmail(new EmailAddress(registrationForm.username()));
        user.setFullName(registrationForm.fullName());
        user.setAddress("");
        user.setRole(RoleConstants.normalizeRole(role));
        if (registrationForm.phone() != null && !registrationForm.phone().trim().isEmpty()) {
            user.setMobile(new com.nlu.identity.domain.vo.PhoneNumber(registrationForm.phone()));
        } else {
            user.setMobile(null);
        }
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }
}
