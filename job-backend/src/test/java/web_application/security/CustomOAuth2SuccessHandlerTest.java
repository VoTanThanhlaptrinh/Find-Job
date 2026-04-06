package web_application.security;

import com.job_web.data.UserRepository;
import com.job_web.models.User;
import com.job_web.security.CustomOAuth2SuccessHandler;
import com.job_web.service.security.JwtService;
import com.job_web.service.security.RefreshTokenService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomOAuth2SuccessHandlerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final CustomOAuth2SuccessHandler handler = new CustomOAuth2SuccessHandler(
            userRepository,
            jwtService,
            refreshTokenService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void oauthSuccessStoresDatabaseUserAuthoritiesInSecurityContext() throws IOException, ServletException {
        ReflectionTestUtils.setField(handler, "isSecure", false);

        User user = new User();
        user.setEmail("user@test.com");
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setActive(true);

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "user@test.com", "name", "OAuth User"),
                "email"
        );
        Authentication authentication = new OAuth2AuthenticationToken(
                oauth2User,
                List.of(),
                "google"
        );

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("user@test.com")).thenReturn("refresh-token");

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                authentication
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
