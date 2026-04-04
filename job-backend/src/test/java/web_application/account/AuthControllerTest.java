package web_application.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.controller.account.AuthController;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private static final String BASE_URL = "/api/auth";

    @Nested
    @DisplayName("POST /api/auth/login - Dang nhap")
    class LoginTests {

        @Test
        @DisplayName("AU01: Dang nhap thanh cong voi endpoint public")
        void login_Success_AnonymousAllowed() throws Exception {
            LoginDTO dto = new LoginDTO("USER", "user@test.com", "password123");
            ApiResponse<String> response = new ApiResponse<>("success", "jwt-token", HttpStatus.OK.value());
            when(authService.login(any(LoginDTO.class), any(), any())).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data").value("jwt-token"));
        }

        @Test
        @DisplayName("AU02: Tra ve 400 khi username rong")
        void login_UsernameBlank() throws Exception {
            LoginDTO dto = new LoginDTO("USER", "", "password123");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("AU03: Endpoint Google URL cho phep anonymous truy cap")
        void googleUrl_PublicEndpoint() throws Exception {
            mockMvc.perform(get(BASE_URL + "/google/url"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data", endsWith("/oauth2/authorization/google")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/password/reset - Dat lai mat khau")
    class ResetPasswordTests {

        @Test
        @DisplayName("AU04: Tra ve 400 khi confirm password khong khop")
        void resetPassword_ValidationError() throws Exception {
            ResetDTO dto = new ResetDTO("newPassword1", "newPassword2", "random-token");

            mockMvc.perform(patch(BASE_URL + "/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
