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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho UserResumeController - các API xem và tải resume.
 * 
 * Test cases bao gồm:
 * - Authentication: Kiểm tra user đã đăng nhập
 * - Authorization: Kiểm tra quyền truy cập resume của chính mình
 * - Validation: Kiểm tra ID hợp lệ
 * - Error handling: Resume không tồn tại
 */
@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class UserResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    private static final String BASE_URL = "/api/user/resumes";
    private static final long VALID_RESUME_ID = 1L;
    private static final long INVALID_RESUME_ID = 999L;
    private static final String TEST_USER_EMAIL = "user@test.com";
    private static final String OTHER_USER_EMAIL = "other@test.com";
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
    @DisplayName("GET /api/user/resumes/{id}/view - Xem Resume")
    class GetResumeViewUrlTests {

        @Test
        @DisplayName("U01: Xem resume thành công khi đã đăng nhập và là chủ sở hữu")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeViewUrl_Success() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeViewUrl(eq(VALID_RESUME_ID), any())).thenReturn(response);

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
        @DisplayName("U02: Trả về 401 khi chưa đăng nhập")
        void getResumeViewUrl_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("U03: Trả về 404 khi resume không tồn tại")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeViewUrl_ResumeNotFound() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
            when(resumeService.getResumeViewUrl(eq(INVALID_RESUME_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + INVALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resume not found."))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("U04: Trả về 403 khi user cố xem resume của người khác")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeViewUrl_Forbidden() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>(
                    "You do not have permission to view this resume.", 
                    null, 
                    HttpStatus.FORBIDDEN.value()
            );
            when(resumeService.getResumeViewUrl(eq(VALID_RESUME_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You do not have permission to view this resume."));
        }

        @Test
        @DisplayName("U09: Trả về 404 khi ID = 0 (invalid)")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeViewUrl_InvalidIdZero() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
            when(resumeService.getResumeViewUrl(eq(0L), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/0/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("U10: Trả về 403 khi HIRER truy cập endpoint của USER")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void getResumeViewUrl_HirerAccessUserEndpoint() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/view")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/user/resumes/{id}/download - Tải Resume")
    class GetResumeDownloadUrlTests {

        @Test
        @DisplayName("U05: Tải resume thành công khi đã đăng nhập và là chủ sở hữu")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeDownloadUrl_Success() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeDownloadUrl(eq(VALID_RESUME_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.resumeId").value(VALID_RESUME_ID))
                    .andExpect(jsonPath("$.data.url").value(TEST_PRESIGNED_URL))
                    .andExpect(jsonPath("$.data.fileName").value(TEST_FILE_NAME));
        }

        @Test
        @DisplayName("U06: Trả về 401 khi chưa đăng nhập")
        void getResumeDownloadUrl_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("U07: Trả về 404 khi resume không tồn tại")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeDownloadUrl_ResumeNotFound() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
            when(resumeService.getResumeDownloadUrl(eq(INVALID_RESUME_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + INVALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resume not found."));
        }

        @Test
        @DisplayName("U08: Trả về 403 khi user cố tải resume của người khác")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void getResumeDownloadUrl_Forbidden() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>(
                    "You do not have permission to download this resume.", 
                    null, 
                    HttpStatus.FORBIDDEN.value()
            );
            when(resumeService.getResumeDownloadUrl(eq(VALID_RESUME_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + VALID_RESUME_ID + "/download")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You do not have permission to download this resume."));
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Response JSON format đúng chuẩn")
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        void responseFormat_CorrectStructure() throws Exception {
            ApiResponse<ResumeUrlDTO> response = new ApiResponse<>("success", validResumeUrlDTO, HttpStatus.OK.value());
            when(resumeService.getResumeViewUrl(eq(VALID_RESUME_ID), any())).thenReturn(response);

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
