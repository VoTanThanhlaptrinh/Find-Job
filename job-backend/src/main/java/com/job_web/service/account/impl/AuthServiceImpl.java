package com.job_web.service.account.impl;

import com.job_web.constant.RoleConstants;
import com.job_web.data.HirerRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.message.MailMessage;
import com.job_web.exception.AppException;
import com.job_web.exception.BadRequestException;
import com.job_web.exception.ForbiddenException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.exception.UnauthorizedException;
import com.job_web.message.MessageProducer;
import com.job_web.models.Hirer;
import com.job_web.models.User;
import com.job_web.service.account.AuthService;
import com.job_web.service.security.JwtService;
import com.job_web.service.security.RefreshTokenService;
import com.job_web.service.support.ReferenceService;
import com.job_web.service.support.SpamService;
import com.job_web.service.support.VerificationService;
import com.job_web.utills.MessageUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final HirerRepository hirerRepository;
    private final ReferenceService refService;
    private final VerificationService verifyService;
    private final MessageProducer messageProducer;
    private final RefreshTokenService refreshTokenService;
    private final SpamService spamService;
    private final JwtService jwtService;
    private final Environment environment;

    @Value("${app.cookie.secure}")
    private boolean isSecure;

    @Value("${application.service.impl.subject-verify}")
    private String subjectVerify;

    @Value("${application.service.impl.email-verify}")
    private String textVerify;

    @Override
    @Transactional
    public String registerUser(RegistationForm registationForm) {
        return registerByRole(registationForm, RoleConstants.USER);
    }

    @Override
    @Transactional
    public String registerHirer(RegistationForm registationForm) {
        return registerByRole(registationForm, RoleConstants.HIRER);
    }

    @Override
    public void sendLinkActivate(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("auth.user.not_found"));
        String link = createLink(user);

        String text = String.format(textVerify, link);
        MailMessage mailMessage = new MailMessage(email, subjectVerify, text);
        try {
            messageProducer.sendMail(mailMessage);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            throw new AppException("auth.email.send_failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void activeAccount(String token) {
        final String activate = jwtService.extractUsername(token);
        String[] values = StringUtils.delimitedListToStringArray(activate, "|");
        User user = userRepository.findByEmail(values[0])
                .orElseThrow(() -> new BadRequestException("auth.user.not_found"));
                
        if (!jwtService.isTokenValid(token, user)) {
            throw new BadRequestException("auth.token.expired");
        }
    }

    @Override
    public String loginUser(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        return loginByRole(loginDTO, request, response, RoleConstants.USER);
    }

    @Override
    public String loginHirer(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        return loginByRole(loginDTO, request, response, RoleConstants.HIRER);
    }

    private String loginByRole(LoginDTO loginDTO,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               String expectedRole) {
        String ip = getClientIP(request);
        if (spamService.checkIpSpamLogin(ip)) {
            throw new AppException(spamService.getMessageLoginSpam(ip), HttpStatus.TOO_MANY_REQUESTS);
        }
        User user = userRepository.findByEmail(loginDTO.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("auth.email.not_found"));

        validateAccountRole(user, expectedRole);

        if (!encoder.matches(loginDTO.getPassword(), user.getPassword())) {
            spamService.addIpSpamLogin(ip);
            throw new BadRequestException("auth.login.wrong_password");
        }
        spamService.deleteIpSpamLogin(ip);
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(loginDTO.getUsername());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7).getSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return accessToken;
    }

    @Override
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BadRequestException("auth.cookie.expired");
        }
        for (Cookie cookie : cookies) {
            if (!"refreshToken".equals(cookie.getName())) {
                continue;
            }
            String token = cookie.getValue();
            if (refreshTokenService.isValid(token)) {
                String username = jwtService.extractUsername(token);
                String accessToken = jwtService.generateToken(username);
                String refreshTokenVal = refreshTokenService.reGenerateRefreshToken(token);
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshTokenVal)
                        .httpOnly(true)
                        .secure(isSecure)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(Duration.ofDays(7).getSeconds())
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                return accessToken;
            }
            throw new ResourceNotFoundException("auth.session.expired");
        }
        throw new ResourceNotFoundException("auth.refresh_token.not_found");
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        request.getSession().invalidate();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (!"refreshToken".equals(cookie.getName())) {
                    continue;
                }
                String token = cookie.getValue();
                refreshTokenService.deleteRefreshToken(token);
                Cookie refreshCookie = new Cookie("refreshToken", null);
                refreshCookie.setPath("/");
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(request.isSecure());
                refreshCookie.setMaxAge(0);
                response.addCookie(refreshCookie);
            }
        }
        response.setHeader("Authorization", "");
    }

    @Override
    public void sendCodeForgotPassword(HttpServletRequest request, String email) {
        String ip = getClientIP(request);
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("auth.email.empty");
        }
        if (spamService.checkIpSpamEmail(ip)) {
            throw new BadRequestException(spamService.getMessageEmailSpam(ip));
        }
        spamService.addIpSpamEmail(ip);

        if (userRepository.findByEmail(email).isEmpty()) {
            throw new BadRequestException("auth.email.not_found");
        }
        String refCode = refService.getRef(6).toUpperCase();
        String subject = MessageUtils.getMessage("auth.forgot_password.subject");
        String body = MessageUtils.getMessage("auth.forgot_password.body", refCode);
        MailMessage mailMessage = new MailMessage(email, subject, body);
        messageProducer.sendMail(mailMessage);

        verifyService.add("ref-email:" + email, refCode, 60 * 5);
    }

    @Override
    public String forgotPassword(ForgotPassDTO forgotPassDTO) {
        if (!verifyService.containsKey("ref-email:" + forgotPassDTO.getEmail())) {
            throw new BadRequestException("auth.verification.code_expired");
        }
        if (!verifyService.getValue("ref-email:" + forgotPassDTO.getEmail()).equals(forgotPassDTO.getCode())) {
            throw new BadRequestException("auth.verification.code_mismatch");
        }
        String random = UUID.randomUUID().toString();
        verifyService.add("random:" + random, forgotPassDTO.getEmail(), 5 * 60);
        verifyService.add(random, forgotPassDTO.getEmail(), 5 * 60);
        return random;
    }

    @Override
    public void checkRandom(String random) {
        if (!StringUtils.hasText(random)) {
            throw new BadRequestException("auth.verification.code_not_found");
        }
        if (!verifyService.containsKey("random:" + random)) {
            throw new BadRequestException("auth.verification.email_required");
        }
    }

    @Override
    public void resetPassword(ResetDTO resetDTO) {
        if (!verifyService.containsKey("random:" + resetDTO.getRandom())) {
            throw new BadRequestException("auth.verification.code_expired");
        }
        String email = verifyService.getValue(resetDTO.getRandom()).toString();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("auth.verification.failed"));
                
        user.setPassword(encoder.encode(resetDTO.getNewPass()));
        userRepository.save(user);
        verifyService.delete("ref-email:" + email);
        verifyService.delete("random:" + resetDTO.getRandom());
        verifyService.delete(resetDTO.getRandom());
    }

    @Override
    public String checkLogin(User user) {
        if (user == null) {
            throw new UnauthorizedException("message.unauthorized");
        }
        return user.getEmail();
    }

    private String registerByRole(RegistationForm registationForm, String role) {
        User user = registationForm.toUser(encoder, role);
        userRepository.saveAndFlush(user);
        if (RoleConstants.ROLE_HIRER.equals(RoleConstants.normalizeRole(role))) {
            createHirer(user);
        }
        sendLinkActivate(user.getUsername());
        return user.getUsername();
    }

    private void createHirer(User user) {
        Hirer hirer = new Hirer();
        Instant now = Instant.now();
        hirer.setUser(user);
        hirer.setCompanyName(user.getFullName());
        hirer.setDescription("");
        hirer.setSocialLink("");
        hirer.setCreateDate(now);
        hirer.setModifiedDate(now);
        hirer.setAddresses(new ArrayList<>());
        hirerRepository.save(hirer);
    }

    private void validateAccountRole(User user, String expectedRole) {
        String normalizedExpectedRole = RoleConstants.normalizeRole(expectedRole);
        boolean matched = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(RoleConstants::normalizeRole)
                .anyMatch(normalizedExpectedRole::equals);
        if (!matched) {
            throw new ForbiddenException(getAccountRoleMismatchMessage(expectedRole));
        }
    }

    private String getInvalidRequestRoleMessage(String expectedRole) {
        return RoleConstants.ROLE_HIRER.equals(RoleConstants.normalizeRole(expectedRole))
                ? "auth.login.hirer.role_invalid"
                : "auth.login.user.role_invalid";
    }

    private String getAccountRoleMismatchMessage(String expectedRole) {
        return RoleConstants.ROLE_HIRER.equals(RoleConstants.normalizeRole(expectedRole))
                ? "auth.login.hirer.account_type_invalid"
                : "auth.login.user.account_type_invalid";
    }

    private String createLink(User user) {
        String token = jwtService.generateToken(user.getUsername() + "|activate");
        String baseUrl = isDevProfile() 
                ? "http://localhost:4200" 
                : "https://find-job-frontend.vercel.app";
        return baseUrl + "/activate?token=" + token;
    }

    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
