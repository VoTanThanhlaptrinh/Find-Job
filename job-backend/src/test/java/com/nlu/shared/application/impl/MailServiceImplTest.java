package com.nlu.shared.application.impl;

import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.impl.AccountServiceImpl;
import com.nlu.identity.application.impl.AuthServiceImpl;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.shared.application.MailService;
import com.nlu.shared.application.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class MailServiceImplTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private MailServiceImpl mailService;

    private static final String BASE_URL = "https://mail-service-tau.vercel.app/";
    private static final String API_KEY = "msk_test_valid_key_123";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        mailService = new MailServiceImpl(restClientBuilder.build(), BASE_URL, API_KEY);
    }

    @Test
    @DisplayName("TC-API-01: Gửi mail thành công qua Mail Service API trả về 201 Created")
    void shouldSendMailSuccessfully() {
        mockServer.expect(requestTo("https://mail-service-tau.vercel.app/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.to").value("user@test.com"))
                .andExpect(jsonPath("$.subject").value("Kích hoạt tài khoản"))
                .andExpect(jsonPath("$.body").value("Vui lòng click vào link"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"mail_123\",\"status\":\"SENT\"}"));

        assertDoesNotThrow(() ->
                mailService.sendMessage("user@test.com", "Kích hoạt tài khoản", "Vui lòng click vào link"));

        mockServer.verify();
    }

    @Test
    @DisplayName("TC-AUTH-01: Header Authorization luôn đính kèm đúng Bearer API Key")
    void shouldIncludeBearerApiKeyInAuthorizationHeader() {
        String customKey = "msk_test_custom_token_xyz";
        MailServiceImpl customService = new MailServiceImpl(restClientBuilder.build(), BASE_URL, customKey);

        mockServer.expect(requestTo("https://mail-service-tau.vercel.app/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + customKey))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"SENT\"}"));

        assertDoesNotThrow(() ->
                customService.sendMessage("candidate@test.com", "Thông báo", "Nội dung thông báo"));

        mockServer.verify();
    }

    @Test
    @DisplayName("TC-API-01 (URL Normalization): Chuẩn hóa base-url có hậu tố /api hoặc không có /api")
    void shouldNormalizeBaseUrlCorrectly() {
        // Base-url ending with /api
        RestClient.Builder builder2 = RestClient.builder();
        MockRestServiceServer server2 = MockRestServiceServer.bindTo(builder2).build();
        MailServiceImpl serviceWithApi = new MailServiceImpl(builder2.build(), "http://localhost:4000/api", API_KEY);

        server2.expect(requestTo("http://localhost:4000/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"SENT\"}"));

        assertDoesNotThrow(() ->
                serviceWithApi.sendMessage("user@test.com", "Subj", "Body"));
        server2.verify();

        // Base-url ending with /api/
        RestClient.Builder builder3 = RestClient.builder();
        MockRestServiceServer server3 = MockRestServiceServer.bindTo(builder3).build();
        MailServiceImpl serviceWithApiSlash = new MailServiceImpl(builder3.build(), "http://localhost:4000/api/", API_KEY);

        server3.expect(requestTo("http://localhost:4000/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"SENT\"}"));

        assertDoesNotThrow(() ->
                serviceWithApiSlash.sendMessage("user@test.com", "Subj", "Body"));
        server3.verify();
    }

    @Test
    @DisplayName("TC-VAL-01: Chặn gửi khi thiếu hoặc rỗng MAIL_SERVICE_API_KEY")
    void shouldThrowExceptionWhenApiKeyIsMissingOrBlank() {
        MailServiceImpl emptyKeyService = new MailServiceImpl(restClientBuilder.build(), BASE_URL, "");
        IllegalStateException exEmpty = assertThrows(IllegalStateException.class, () ->
                emptyKeyService.sendMessage("user@test.com", "Tiêu đề", "Nội dung"));
        assertThat(exEmpty.getMessage()).contains("MAIL_SERVICE_API_KEY is not configured");

        MailServiceImpl nullKeyService = new MailServiceImpl(restClientBuilder.build(), BASE_URL, null);
        IllegalStateException exNull = assertThrows(IllegalStateException.class, () ->
                nullKeyService.sendMessage("user@test.com", "Tiêu đề", "Nội dung"));
        assertThat(exNull.getMessage()).contains("MAIL_SERVICE_API_KEY is not configured");
    }

    @Test
    @DisplayName("TC-VAL-02: Chặn gửi khi to, subject hoặc text rỗng/null bằng IllegalArgumentException")
    void shouldThrowIllegalArgumentExceptionWhenInputIsInvalid() {
        // 'to' invalid
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage(null, "Subject", "Body"));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("", "Subject", "Body"));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("   ", "Subject", "Body"));

        // 'subject' invalid
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", null, "Body"));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", "", "Body"));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", "   ", "Body"));

        // 'text' invalid
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", "Subject", null));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", "Subject", ""));
        assertThrows(IllegalArgumentException.class, () -> mailService.sendMessage("user@test.com", "Subject", "   "));
    }

    @Test
    @DisplayName("TC-VAL-03: Xử lý lỗi khi Mail Service trả về 502 Bad Gateway")
    void shouldThrowIllegalStateExceptionOn502BadGateway() {
        mockServer.expect(requestTo("https://mail-service-tau.vercel.app/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"SMTP connection timeout\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                mailService.sendMessage("user@test.com", "Tiêu đề", "Nội dung"));

        assertThat(ex.getMessage()).contains("502");
        assertThat(ex.getMessage()).contains("SMTP connection timeout");
        assertThat(ex.getCause()).isNotNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("TC-VAL-03: Xử lý lỗi khi Mail Service trả về 401 Unauthorized")
    void shouldThrowIllegalStateExceptionOn401Unauthorized() {
        mockServer.expect(requestTo("https://mail-service-tau.vercel.app/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Invalid API Key\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                mailService.sendMessage("user@test.com", "Tiêu đề", "Nội dung"));

        assertThat(ex.getMessage()).contains("401");
        assertThat(ex.getCause()).isNotNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("TC-VAL-03: Xử lý lỗi khi Mail Service trả về 400 Bad Request")
    void shouldThrowIllegalStateExceptionOn400BadRequest() {
        mockServer.expect(requestTo("https://mail-service-tau.vercel.app/api/mails/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Invalid recipient email format\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                mailService.sendMessage("invalid-email", "Tiêu đề", "Nội dung"));

        assertThat(ex.getMessage()).contains("400");
        assertThat(ex.getCause()).isNotNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("TC-FUNC-01: AuthServiceImpl và AccountServiceImpl gọi trực tiếp MailService.sendMessage thay vì MessageProducer")
    void shouldVerifyDirectMailServiceCallInAuthAndAccountServices() {
        MailService mockMail = Mockito.mock(MailService.class);
        UserRepository mockUserRepo = Mockito.mock(UserRepository.class);
        JwtService mockJwtService = Mockito.mock(JwtService.class);
        VerificationService mockVerifyService = Mockito.mock(VerificationService.class);

        // 1. Kiểm thử AuthServiceImpl.sendLinkActivate gọi trực tiếp mailService.sendMessage(...)
        AuthServiceImpl authService = new AuthServiceImpl(
                null,
                mockUserRepo,
                null,
                null,
                mockVerifyService,
                mockMail,
                null,
                null,
                null,
                mockJwtService,
                null
        );
        ReflectionTestUtils.setField(authService, "url", "https://frontend.example.com");
        ReflectionTestUtils.setField(authService, "subjectVerify", "Kích hoạt tài khoản");
        ReflectionTestUtils.setField(authService, "textVerify", "Link: %s");

        User user = new User();
        user.setId(101L);
        user.setEmail(new EmailAddress("candidate@test.com"));
        when(mockUserRepo.findByEmail_Value("candidate@test.com")).thenReturn(Optional.of(user));
        when(mockJwtService.generateActivationToken("candidate@test.com")).thenReturn("act-token-abc");

        authService.sendLinkActivate("candidate@test.com");

        verify(mockMail).sendMessage(
                eq("candidate@test.com"),
                eq("Kích hoạt tài khoản"),
                eq("Link: https://frontend.example.com/activate?token=act-token-abc")
        );

        // 2. Kiểm thử AccountServiceImpl.checkOauth2 gọi trực tiếp mailService.sendMessage(...)
        AccountServiceImpl accountService = new AccountServiceImpl(
                null,
                mockUserRepo,
                mockVerifyService,
                mockMail,
                null,
                null
        );
        ReflectionTestUtils.setField(accountService, "url", "https://frontend.example.com");
        ReflectionTestUtils.setField(accountService, "subjectOauth", "Thiết lập mật khẩu");
        ReflectionTestUtils.setField(accountService, "textOauth", "Email %s link: %s");

        User oauthUser = new User();
        oauthUser.setId(202L);
        oauthUser.setEmail(new EmailAddress("oauth@test.com"));
        oauthUser.setOauth2Enabled(true);
        oauthUser.setPassword(null);
        when(mockUserRepo.findById(202L)).thenReturn(Optional.of(oauthUser));

        boolean res = accountService.checkOauth2(oauthUser);
        assertThat(res).isFalse();

        verify(mockMail).sendMessage(
                eq("oauth@test.com"),
                eq("Thiết lập mật khẩu"),
                contains("https://frontend.example.com/reset-pass/")
        );
    }
}
