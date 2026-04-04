package web_application.application;

import com.job_web.controller.application.HirerApplicationController;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.application.ApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HirerApplicationController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class HirerApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplyService applyService;

    private static final String BASE_URL = "/api/hirer/applications";

    @Nested
    @DisplayName("GET /api/hirer/applications/jobs/{jobId}/candidates/{pageIndex}/{pageSize}")
    class CandidateTests {

        @Test
        @DisplayName("HA01: Lay danh sach ung vien thanh cong voi quyen HIRER")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void listCandidates_Success() throws Exception {
            CandidateDTO candidate = new CandidateDTO() {
                @Override
                public String getFullName() {
                    return "Nguyen Van B";
                }

                @Override
                public String getEmail() {
                    return "candidate@test.com";
                }

                @Override
                public String getFileName() {
                    return "resume.pdf";
                }

                @Override
                public String getApplyDate() {
                    return "2026-04-04";
                }
            };
            Page<CandidateDTO> page = new PageImpl<>(List.of(candidate));
            ApiResponse<Page<CandidateDTO>> response = new ApiResponse<>("success", page, HttpStatus.OK.value());
            when(applyService.getAllCandidateAppliedJob(eq(0), eq(10), eq(1L))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/jobs/1/candidates/0/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.content[0].email").value("candidate@test.com"));
        }

        @Test
        @DisplayName("HA02: Tra ve 400 khi pageIndex khong phai so hop le")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void listCandidates_InvalidPageIndex() throws Exception {
            mockMvc.perform(get(BASE_URL + "/jobs/1/candidates/abc/10"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("HA03: Tra ve 401 khi chua dang nhap")
        void listCandidates_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/jobs/1/candidates/0/10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("HA04: Tra ve 403 khi USER truy cap endpoint cua HIRER")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void listCandidates_ForbiddenForUser() throws Exception {
            mockMvc.perform(get(BASE_URL + "/jobs/1/candidates/0/10"))
                    .andExpect(status().isForbidden());
        }
    }
}
