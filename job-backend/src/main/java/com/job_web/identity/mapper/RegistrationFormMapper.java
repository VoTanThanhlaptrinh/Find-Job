package com.job_web.identity.mapper;

import com.job_web.identity.domain.vo.EmailAddress;
import com.job_web.identity.domain.vo.Password;
import com.job_web.identity.domain.vo.RoleConstants;
import com.job_web.identity.api.dto.RegistationForm;
import com.job_web.identity.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RegistrationFormMapper {

    public User toUser(RegistationForm registationForm, PasswordEncoder passwordEncoder, String role) {
        User user = new User();
        user.setPassword(new Password(passwordEncoder.encode(registationForm.password())));
        user.setEmail(new EmailAddress(registationForm.username()));
        user.setFullName(registationForm.fullName());
        user.setAddress("");
        user.setRole(RoleConstants.normalizeRole(role));
        user.setMobile(null);
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }
}
