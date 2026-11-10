package web_application.account;

import com.nlu.identity.api.dto.HirerRegistrationForm;
import com.nlu.identity.application.JwtFamilyService;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.domain.vo.RoleConstants;
import com.nlu.identity.mapper.RegistrationFormMapper;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.identity.api.dto.LoginDTO;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.application.impl.AuthServiceImpl;
import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.RefreshTokenService;
import com.nlu.shared.application.ReferenceService;
import com.nlu.shared.application.SpamService;
import com.nlu.shared.application.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        ReflectionTestUtils.setField(authService, "isSecure", false);
        ReflectionTestUtils.setField(authService, "url", "http://localhost:4200");
        ReflectionTestUtils.setField(authService, "subjectVerify", "Verify");
        ReflectionTestUtils.setField(authService, "textVerify", "Link: %s");
    }

    @Test
    @DisplayName("AS01: Dang ky hirer tao user role HIRER va tao ban ghi Hirer + token kich hoat rieng")
    void registerHirer_CreatesHirerFlow() {
        HirerRegistrationForm form = new HirerRegistrationForm("Recruiter Test", "hirer@test.com", "password123", "password123", "", "ACME");
        User mappedUser = new User();
        mappedUser.setEmail(new EmailAddress("hirer@test.com"));
        mappedUser.setFullName("Recruiter Test");
        mappedUser.setRole(RoleConstants.ROLE_HIRER);
        mappedUser.setEnabled(true);
        mappedUser.setActive(false);

        when(mapper.toUser(eq(form), eq(encoder), eq(RoleConstants.HIRER))).thenReturn(mappedUser);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail_Value("hirer@test.com")).thenReturn(Optional.of(mappedUser));
        when(jwtService.generateActivationToken("hirer@test.com")).thenReturn("activate-token");

        String username = authService.registerHirer(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(RoleConstants.ROLE_HIRER, userCaptor.getValue().getRole());
        verify(recruitmentRepository).save(any(Recruitment.class));
        verify(jwtService).generateActivationToken("hirer@test.com");
        verify(messageProducer).sendMail(any());
        assertEquals("hirer@test.com", username);
    }

    @Test
    @DisplayName("AS02: Kich hoat tai khoan thanh cong voi activation token hop le")
    void activeAccount_ValidToken_ActivatesUser() {
        User user = new User();
        user.setEmail(new EmailAddress("user@test.com"));
        user.setActive(false);
        user.setEnabled(true);

        when(jwtService.extractUsername("valid-act-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail_Value("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isActivationTokenValid("valid-act-token", "user@test.com")).thenReturn(true);

        authService.activeAccount("valid-act-token");

        assertTrue(user.isActive());
        verify(userRepository).save(user);
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

    @Test
    @DisplayName("AS04: Login bi tu choi khi tai khoan bi khoa (accountLocked = true)")
    void loginUser_LockedAccountRejected() {
        LoginDTO dto = new LoginDTO("user@test.com", "password123");
        User user = new User();
        user.setEmail(new EmailAddress("user@test.com"));
        user.setPassword(new Password("encoded"));
        user.setRole(RoleConstants.ROLE_USER);
        user.setAccountLocked(true);
        user.setEnabled(true);
        user.setActive(true);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(spamService.checkIpSpamLogin("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail_Value("user@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "encoded")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> authService.loginUser(dto, request, new MockHttpServletResponse()));

        assertEquals("auth.account.locked", ex.getMessage());
    }

    @Test
    @DisplayName("AS05: Login bi tu choi khi tai khoan chua kich hoat email (active = false)")
    void loginUser_InactiveAccountRejected() {
        LoginDTO dto = new LoginDTO("user@test.com", "password123");
        User user = new User();
        user.setEmail(new EmailAddress("user@test.com"));
        user.setPassword(new Password("encoded"));
        user.setRole(RoleConstants.ROLE_USER);
        user.setAccountLocked(false);
        user.setEnabled(true);
        user.setActive(false);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(spamService.checkIpSpamLogin("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail_Value("user@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "encoded")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> authService.loginUser(dto, request, new MockHttpServletResponse()));

        assertEquals("auth.account.not_activated", ex.getMessage());
    }
}
