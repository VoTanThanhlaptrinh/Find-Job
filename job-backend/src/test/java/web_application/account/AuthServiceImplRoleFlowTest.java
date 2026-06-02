package web_application.account;

import com.job_web.identity.domain.vo.EmailAddress;
import com.job_web.identity.domain.vo.RoleConstants;
import com.job_web.recruiment.domain.model.Recruitment;
import com.job_web.recruiment.domain.repository.RecruitmentRepository;
import com.job_web.identity.domain.repository.UserRepository;
import com.job_web.identity.api.dto.LoginDTO;
import com.job_web.identity.api.dto.RegistationForm;
import com.job_web.shared.domain.exception.BadRequestException;
import com.job_web.shared.domain.exception.ForbiddenException;
import com.job_web.shared.infrastructure.message.MessageProducer;
import com.job_web.identity.domain.model.User;
import com.job_web.identity.application.impl.AuthServiceImpl;
import com.job_web.identity.application.JwtService;
import com.job_web.identity.application.RefreshTokenService;
import com.job_web.shared.application.ReferenceService;
import com.job_web.shared.application.SpamService;
import com.job_web.shared.application.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRoleFlowTest {

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
    private SpamService spamService;

    @Mock
    private JwtService jwtService;

    @Mock
    private Environment environment;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "isSecure", false);
        ReflectionTestUtils.setField(authService, "subjectVerify", "Verify");
        ReflectionTestUtils.setField(authService, "textVerify", "Link: %s");
    }

    @Test
    @DisplayName("AS01: Dang ky hirer tao user role HIRER va tao ban ghi Hirer")
    void registerHirer_CreatesHirerFlow() {
        RegistationForm form = new RegistationForm("Recruiter Test", "hirer@test.com", "password123", "password123");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(encoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail_Value("hirer@test.com")).thenAnswer(invocation -> {
            User user = new User();
            user.setEmail(new EmailAddress("hirer@test.com"));
            user.setFullName("Recruiter Test");
            user.setRole(RoleConstants.ROLE_HIRER);
            return Optional.of(user);
        });
        when(jwtService.generateToken("hirer@test.com|activate")).thenReturn("activate-token");

        String username = authService.registerHirer(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(RoleConstants.ROLE_HIRER, userCaptor.getValue().getRole());
        verify(recruitmentRepository).save(any(Recruitment.class));
        verify(messageProducer).sendMail(any());
        assertEquals("hirer@test.com", username);
    }

    @Test
    @DisplayName("AS02: User login tu endpoint USER nhung gui role HIRER thi bi chan")
    void loginUser_InvalidRequestRole() {
        LoginDTO dto = new LoginDTO( "user@test.com", "password123");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.loginUser(dto, request, new MockHttpServletResponse()));

        assertEquals("auth.login.user.role_invalid", ex.getMessage());
        verify(userRepository, never()).findByEmail_Value(any());
    }

    @Test
    @DisplayName("AS03: Hirer login endpoint nhung tai khoan USER thi bi chan theo role")
    void loginHirer_UserAccountRejected() {
        LoginDTO dto = new LoginDTO("user@test.com", "password123");
        User user = new User();
        user.setEmail(new EmailAddress("user@test.com"));
        user.setRole(RoleConstants.ROLE_USER);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(spamService.checkIpSpamLogin("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail_Value("user@test.com")).thenReturn(Optional.of(user));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> authService.loginHirer(dto, request, new MockHttpServletResponse()));

        assertEquals("auth.login.hirer.account_type_invalid", ex.getMessage());
        verify(encoder, never()).matches(any(), any());
    }
}
