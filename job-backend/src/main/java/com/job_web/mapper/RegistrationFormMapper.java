package com.job_web.mapper;

import com.job_web.constant.RoleConstants;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.models.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RegistrationFormMapper {

    public User toUser(RegistationForm registationForm, PasswordEncoder passwordEncoder, String role) {
        User user = new User();
        user.setPassword(new com.job_web.models.vo.Password(passwordEncoder.encode(registationForm.password())));
        user.setEmail(new com.job_web.models.vo.EmailAddress(registationForm.username()));
        user.setFullName(registationForm.fullName());
        user.setAddress("");
        user.setRole(RoleConstants.normalizeRole(role));
        user.setMobile(null);
        user.setActive(false);
        user.setEnabled(true);
        return user;
    }
}
