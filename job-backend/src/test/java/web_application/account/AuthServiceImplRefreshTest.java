package web_application.account;

import com.nlu.identity.application.JwtFamilyService;
import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.RefreshTokenService;
import com.nlu.identity.application.impl.AuthServiceImpl;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.RoleConstants;
import com.nlu.identity.mapper.RegistrationFormMapper;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.shared.application.ReferenceService;
import com.nlu.shared.application.SpamService;
import com.nlu.shared.application.VerificationService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRefreshTest {

    @Mock
    private PasswordEncoder encoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecruitmentRepository recruitmentRepository;
    @Mock
    private ReferenceService refService;
    @Mock
    private VerificationService verifyService;
    @Mock
    private MessageProducer messageProducer;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtFamilyService jwtFamilyService;
    @Mock
    private SpamService spamService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RegistrationFormMapper mapper;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "isSecure", true);
        ReflectionTestUtils.setField(authService, "url", "http://localhost:4200");
        ReflectionTestUtils.setField(authService, "subjectVerify", "Verify");
        ReflectionTestUtils.setField(authService, "textVerify", "Link: %s");
        ReflectionTestUtils.setField(authService, "refreshTokenTtlSeconds", 604800L);
    }

    @Test
    void refreshToken_ValidCookie_RotatesAndWritesConfiguredTtlResponseCookie() {
        User user = activeCandidate();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(authService, "refreshTokenTtlSeconds", 1_209_600L);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("JSESSIONID", "abc"),
                new Cookie("refreshToken", "old-rt")
        });
        when(refreshTokenService.isValid("old-rt")).thenReturn(true);
        when(jwtService.extractUsername("old-rt")).thenReturn("user@test.com");
        when(userRepository.findByEmail_Value("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.reGenerateRefreshToken("old-rt")).thenReturn("new-rt");

        String accessToken = authService.refreshToken(request, response);

        assertThat(accessToken).isEqualTo("new-access-token");
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=new-rt");
        assertThat(setCookie).contains("Max-Age=1209600");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    void refreshToken_MissingRefreshCookieWithOtherCookies_ThrowsNotFound() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "abc")});

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.refreshToken(request, response)
        );

        assertThat(ex.getMessage()).isEqualTo("auth.refresh_token.not_found");
    }

    @Test
    void refreshToken_NullCookies_ThrowsCookieExpired() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getCookies()).thenReturn(null);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.refreshToken(request, response)
        );

        assertThat(ex.getMessage()).isEqualTo("auth.cookie.expired");
    }

    @Test
    void refreshToken_InvalidToken_ThrowsSessionExpired() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", "stale-rt")});
        when(refreshTokenService.isValid("stale-rt")).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.refreshToken(request, response)
        );

        assertThat(ex.getMessage()).isEqualTo("auth.session.expired");
    }

    @Test
    void logout_ClearsCookieSymmetricallyWithResponseCookieAndRevokesNonBlankToken() {
        HttpSession session = mock(HttpSession.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", "valid-rt")});

        authService.logout(request, response);

        verify(session).invalidate();
        verify(refreshTokenService).deleteRefreshToken("valid-rt");
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    void logout_BlankCookieSkipsRevocationButClearsCookieHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getSession(false)).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", "")});

        authService.logout(request, response);

        verify(refreshTokenService, never()).deleteRefreshToken("");
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    private User activeCandidate() {
        User user = new User();
        user.setId(1L);
        user.setEmail(new EmailAddress("user@test.com"));
        user.setRole(RoleConstants.ROLE_USER);
        user.setEnabled(true);
        user.setActive(true);
        user.setAccountLocked(false);
        return user;
    }
}

