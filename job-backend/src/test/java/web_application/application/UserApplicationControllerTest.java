package web_application.application;

import com.job_web.controller.application.UserApplicationController;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.application.ApplyService;
import com.job_web.service.job.JobQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserApplicationController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class UserApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplyService applyService;

    @MockBean
    private JobQueryService jobQueryService;

    private static final String BASE_URL = "/api/user";

    @Nested
    @DisplayName("GET /api/user/applications/jobs/{jobId}/status - Kiem tra da ung tuyen")
    class HasAppliedTests {

        @Test
        @DisplayName("UA01: Tra ve trang thai da ung tuyen thanh cong")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void hasApplied_Success() throws Exception {
            ApiResponse<Boolean> response = new ApiResponse<>("success", true, HttpStatus.OK.value());
            when(applyService.hasApplied(any(), eq(1L))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/applications/jobs/1/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("UA02: Tra ve 400 khi jobId khong hop le")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void hasApplied_InvalidJobId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/applications/jobs/invalid/status"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("UA03: Tra ve 401 khi chua dang nhap")
        void hasApplied_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/applications/jobs/1/status"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("UA04: Tra ve 403 khi HIRER truy cap endpoint cua USER")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void hasApplied_ForbiddenForHirer() throws Exception {
            mockMvc.perform(get(BASE_URL + "/applications/jobs/1/status"))
                    .andExpect(status().isForbidden());
        }
    }
}
