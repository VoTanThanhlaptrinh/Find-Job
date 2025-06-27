package com.job_web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.data.UserRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.models.RefreshToken;
import com.job_web.models.User;
import com.job_web.service.JwtService;
import com.job_web.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = authenticationToken.getPrincipal();
        User user = userRepository.findByEmail(principal.getAttribute("email")).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(Objects.requireNonNull(principal.getAttribute("email")).toString());
            user.setActive(true);
            userRepository.save(user);
        }
        String token = jwtService.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.between(Instant.now(), refreshToken.getExpiryDate()).getSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        String redirectUrl = UriComponentsBuilder
//                .fromUriString("http://localhost:4200/login-callback")
//                .queryParam("token", token)
//                .build().toUriString();
//        response.setStatus(HttpStatus.OK.value());
//        response.sendRedirect(redirectUrl);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());  // cài cookie
        response.setContentType("text/html");
        response.getWriter().write(
                "<html><head>"
                        + "<script>window.location.href='http://localhost:4200/login-callback?token=" + token + "';</script>"
                        + "</head><body>"
                        + "Redirecting..."
                        + "</body></html>"
        );
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().flush();
    }
}
