package web_application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.controller.admin.AdminAuthController;
import com.job_web.dto.admin.auth.AdminLoginRequest;
import com.job_web.dto.admin.auth.AdminLoginResponse;
import com.job_web.dto.admin.auth.AdminRefreshRequest;
import com.job_web.service.admin.AdminService;
import jakarta.servlet.http.HttpServletResponse;
import com.job_web.utills.MessageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, AdminAuthController.class, MessageUtils.class})
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    private static final String BASE_URL = "/admin/auth";

    @Nested
    @DisplayName("POST /admin/auth/login")
    class LoginTests {

        @Test
        @DisplayName("AD01: Dang nhap admin thanh cong")
        void login_Success() throws Exception {
            AdminLoginRequest request = new AdminLoginRequest("admin@elitehire.com", "password", true);
            when(adminService.login(any(AdminLoginRequest.class), any(HttpServletResponse.class)))
                    .thenReturn("access-token");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").value("access-token"));
        }
    }

    @Nested
    @DisplayName("POST /admin/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("AD02: Lam moi token thanh cong")
        void refresh_Success() throws Exception {
            AdminRefreshRequest request = new AdminRefreshRequest("old-refresh-token");
            AdminLoginResponse response = AdminLoginResponse.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(900)
                    .build();

            when(adminService.refresh(any(AdminRefreshRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }
    }
}
