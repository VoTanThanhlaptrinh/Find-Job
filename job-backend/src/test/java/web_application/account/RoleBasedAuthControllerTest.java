package web_application.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.JobPortalWebApplication;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.service.account.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class RoleBasedAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("RA01: User login di dung luong /api/auth/user/login")
    void userLogin_UsesUserFlow() throws Exception {
        LoginDTO dto = new LoginDTO("USER", "user@test.com", "password123");
        when(authService.loginUser(any(LoginDTO.class), any(), any())).thenReturn("user-token");

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("user-token"))
                .andExpect(jsonPath("$.status").value(200));

        verify(authService).loginUser(any(LoginDTO.class), any(), any());
    }

    @Test
    @DisplayName("RA02: Hirer login di dung luong /api/auth/hirer/login")
    void hirerLogin_UsesHirerFlow() throws Exception {
        LoginDTO dto = new LoginDTO("HIRER", "hirer@test.com", "password123");
        when(authService.loginHirer(any(LoginDTO.class), any(), any())).thenReturn("hirer-token");

        mockMvc.perform(post("/api/auth/hirer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("hirer-token"))
                .andExpect(jsonPath("$.status").value(200));

        verify(authService).loginHirer(any(LoginDTO.class), any(), any());
    }

    @Test
    @DisplayName("RA03: User register di dung luong /api/auth/user/register")
    void userRegister_UsesUserFlow() throws Exception {
        RegistationForm dto = new RegistationForm("User Test", "user@test.com", "password123", "password123");
        when(authService.registerUser(any(RegistationForm.class))).thenReturn("user@test.com");

        mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("user@test.com"))
                .andExpect(jsonPath("$.status").value(200));

        verify(authService).registerUser(any(RegistationForm.class));
    }

    @Test
    @DisplayName("RA04: Hirer register di dung luong /api/auth/hirer/register")
    void hirerRegister_UsesHirerFlow() throws Exception {
        RegistationForm dto = new RegistationForm("Hirer Test", "hirer@test.com", "password123", "password123");
        when(authService.registerHirer(any(RegistationForm.class))).thenReturn("hirer@test.com");

        mockMvc.perform(post("/api/auth/hirer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("hirer@test.com"))
                .andExpect(jsonPath("$.status").value(200));

        verify(authService).registerHirer(any(RegistationForm.class));
    }
}
