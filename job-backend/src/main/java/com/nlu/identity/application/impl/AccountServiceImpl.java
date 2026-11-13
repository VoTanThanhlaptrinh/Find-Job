package com.nlu.identity.application.impl;

import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.mapper.UserMapper;
import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.identity.api.dto.UserInfo;
import com.nlu.shared.domain.exception.AppException;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.application.AccountService;
import com.nlu.identity.application.JwtFamilyService;
import com.nlu.identity.application.UserDetailsCacheService;
import com.nlu.shared.application.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final VerificationService verifyService;
    private final MessageProducer mailProducer;
    private final UserMapper userMapper;
    private final JwtFamilyService jwtFamilyService;
    private UserDetailsCacheService userDetailsCacheService;

    @Autowired(required = false)
    void setUserDetailsCacheService(UserDetailsCacheService userDetailsCacheService) {
        this.userDetailsCacheService = userDetailsCacheService;
    }

    @Value("${app.frontend-url}")
    private String url;
    private static final String MDC_USER_ID = "userId";

    @Value("${application.service.impl.subject-oauth2}")
    private String subjectOauth;

    @Value("${application.service.impl.email-oauth2}")
    private String textOauth;

    @Override
    public UserInfo getDetailUser(User user) {
        return userMapper.toUserInfo(user);
    }

    @Override
    public void changePassword(String newPassword, String oldPassword, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Password change requested for user: {}", user.getId());
            User persisted = userRepository.findById(user.getId()).orElse(user);

            if (!encoder.matches(oldPassword, persisted.getPassword())) {
                log.warn("Password change failed — current password mismatch for user: {}", user.getId());
                throw new BadRequestException("auth.password.current_mismatch");
            }

            persisted.setPassword(new Password(encoder.encode(newPassword)));
            userRepository.save(persisted);
            jwtFamilyService.revokeAllUserFamilies(persisted.getEmail());
            evictUserDetailsCache(persisted.getEmail());

            log.info("Password changed successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public void updateInfo(UserInfo userInfo, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Profile update requested for user: {}", user.getId());
            User persisted = userRepository.findById(user.getId()).orElse(user);

            userMapper.updateUser(userInfo, persisted);
            userRepository.save(persisted);
            evictUserDetailsCache(persisted.getEmail());

            log.info("Profile updated successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public boolean checkOauth2(User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            User persisted = userRepository.findById(user.getId()).orElse(user);

            if (persisted.isOauth2Enabled() && (persisted.getPassword() == null || persisted.getPassword().isEmpty())) {
                log.info("OAuth2 user detected without password — initiating password setup for user: {}",
                        persisted.getId());

                String random = UUID.randomUUID().toString();
                String link = String.format("%s/reset-pass/%s", url, random);

                String text = String.format(textOauth, persisted.getEmail(), link);
                MailMessage mailMessage = new MailMessage(persisted.getEmail(), subjectOauth, text);
                verifyService.add("random:" + random, persisted.getEmail(), 60 * 60);
                verifyService.add(random, persisted.getEmail(), 60 * 60);

                try {
                    mailProducer.sendMail(mailMessage);
                    log.info("OAuth2 password setup email dispatched for user: {}", persisted.getId());
                } catch (Exception e) {
                    log.warn("Failed to send OAuth2 password setup email for user: {}", persisted.getId());
                    throw new AppException("auth.email.send_failed", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return false;
            }
            return true;
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    private void evictUserDetailsCache(String email) {
        if (userDetailsCacheService != null) {
            userDetailsCacheService.evictByEmail(email);
        }
    }
}
