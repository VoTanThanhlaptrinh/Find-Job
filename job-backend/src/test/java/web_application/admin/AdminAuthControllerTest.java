package web_application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.admin.api.AdminAuthController;
import com.nlu.admin.api.dto.auth.AdminLoginRequest;
import com.nlu.identity.api.dto.LoginDTO;
import com.nlu.identity.application.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.nlu.shared.utils.MessageUtils;
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
    private AuthService authService;

    private static final String BASE_URL = "/admin/auth";

    @Nested
    @DisplayName("POST /admin/auth/login")
    class LoginTests {

        @Test
        @DisplayName("AD01: Dang nhap admin thanh cong")
        void login_Success() throws Exception {
            AdminLoginRequest request = new AdminLoginRequest("admin@elitehire.com", "password");
            when(authService.loginAdmin(any(LoginDTO.class), any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .thenReturn("access-token");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").value("access-token"));
        }
    }
}
