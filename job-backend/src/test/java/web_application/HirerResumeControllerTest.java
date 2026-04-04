package web_application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.JobPortalWebApplication;
import com.job_web.dto.application.ResumeUrlDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.application.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho HirerResumeController - các API xem và tải resume dành cho Hirer.
 * 
 * Test cases bao gồm:
 * - Authentication: Kiểm tra hirer đã đăng nhập
 * - Authorization: Kiểm tra quyền HIRER
 * - Error handling: Resume không tồn tại
 */
@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class HirerResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    private static final String BASE_URL = "/api/hirer/resumes";
    private static final long VALID_RESUME_ID = 1L;
    private static final long INVALID_RESUME_ID = 999L;
    private static final String TEST_HIRER_EMAIL = "hirer@test.com";
    private static final String TEST_PRESIGNED_URL = "https://bucket.r2.cloudflarestorage.com/resume.pdf?signature=abc123";
    private static final String TEST_FILE_NAME = "resume.pdf";

    private ResumeUrlDTO validResumeUrlDTO;

    @BeforeEach
    void setUp() {
        validResumeUrlDTO = new ResumeUrlDTO(
                VALID_RESUME_ID,
                TEST_FILE_NAME,
                TEST_PRESIGNED_URL,
                30
        );
    }

    @Nested
    @DisplayName("GET /api/hirer/resumes/{id}/view - Hirer xem Resume")
    class GetResumeViewUrlTests {

        @Test
        @DisplayName("H01: Xem resume thành công với quyền HIRER")
        @WithMockUser(username = TEST_HIRER_EMAIL, roles = "HIRER")
        void getResumeViewUrl_Success() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeViewUrlForHirer(eq(VALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.resumeId").value(VALID_RESUME_ID))
                    .andExpect(jsonPath("$.data.url").value(TEST_PRESIGNED_URL))
                    .andExpect(jsonPath("$.data.fileName").value(TEST_FILE_NAME))
                    .andExpect(jsonPath("$.data.expiresInMinutes").value(30));
        }

        @Test
        @DisplayName("H02: Trả về 401 khi chưa đăng nhập")
        void getResumeViewUrl_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("H03: Trả về 404 khi resume không tồn tại")
        @WithMockUser(username = TEST_HIRER_EMAIL, roles = "HIRER")
        void getResumeViewUrl_ResumeNotFound() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
            when(resumeService.getResumeViewUrlForHirer(eq(INVALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + INVALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resume not found."))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("H07: Trả về 403 khi USER truy cập endpoint của HIRER")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void getResumeViewUrl_UserAccessHirerEndpoint() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/hirer/resumes/{id}/download - Hirer tải Resume")
    class GetResumeDownloadUrlTests {

        @Test
        @DisplayName("H04: Tải resume thành công với quyền HIRER")
        @WithMockUser(username = TEST_HIRER_EMAIL, roles = "HIRER")
        void getResumeDownloadUrl_Success() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeDownloadUrlForHirer(eq(VALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.resumeId").value(VALID_RESUME_ID))
                    .andExpect(jsonPath("$.data.url").value(TEST_PRESIGNED_URL))
                    .andExpect(jsonPath("$.data.fileName").value(TEST_FILE_NAME));
        }

        @Test
        @DisplayName("H05: Trả về 401 khi chưa đăng nhập")
        void getResumeDownloadUrl_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("H06: Trả về 404 khi resume không tồn tại")
        @WithMockUser(username = TEST_HIRER_EMAIL, roles = "HIRER")
        void getResumeDownloadUrl_ResumeNotFound() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
            when(resumeService.getResumeDownloadUrlForHirer(eq(INVALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + INVALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resume not found."));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("ADMIN có thể truy cập endpoint HIRER")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN", "HIRER"})
        void adminCanAccessHirerEndpoint() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeViewUrlForHirer(eq(VALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Anonymous user không thể truy cập")
        void anonymousCannotAccess() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Response JSON format đúng chuẩn")
        @WithMockUser(username = TEST_HIRER_EMAIL, roles = "HIRER")
        void responseFormat_CorrectStructure() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeViewUrlForHirer(eq(VALID_RESUME_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.status").value(200));
        }
    }
}
