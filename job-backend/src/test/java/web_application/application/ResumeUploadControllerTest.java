package web_application.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.JobPortalWebApplication;
import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.ResumeUploadService;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class ResumeUploadControllerTest {

    static {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeUploadService resumeUploadService;

    private static final String BASE_URL = "/api/user/resume-uploads";
    private static final String TEST_USER_EMAIL = "user@test.com";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = buildUser(1L, TEST_USER_EMAIL, "ROLE_USER");
    }

    private RequestPostProcessor authenticatedUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                testUser,
                null,
                testUser.getAuthorities()
        ));
    }

    private User buildUser(Long id, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setEmail(new EmailAddress(email));
        user.setRole(role);
        return user;
    }

    @Nested
    @DisplayName("POST /api/user/resume-uploads/initiate")
    class InitiateEndpointTests {

        @Test
        @DisplayName("201 Created khi initiate hợp lệ với USER đã đăng nhập")
        void initiateUpload_Success() throws Exception {
            UUID uploadId = UUID.randomUUID();
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest("cv.pdf", "application/pdf", 1048576L);
            ResumeUploadInitiateResponse response = new ResumeUploadInitiateResponse(
                    uploadId,
                    "https://r2.cloudflarestorage.com/temp/resumes/1/" + uploadId,
                    "PUT",
                    Map.of("Content-Type", "application/pdf"),
                    LocalDateTime.now().plusMinutes(10)
            );

            when(resumeUploadService.initiateUpload(any(ResumeUploadInitiateRequest.class), any()))
                    .thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/initiate")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.uploadId").value(uploadId.toString()))
                    .andExpect(jsonPath("$.data.method").value("PUT"))
                    .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty())
                    .andExpect(jsonPath("$.data.requiredHeaders['Content-Type']").value("application/pdf"));
        }

        @Test
        @DisplayName("401 Unauthorized khi chưa đăng nhập")
        void initiateUpload_Unauthorized() throws Exception {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest("cv.pdf", "application/pdf", 1048576L);

            mockMvc.perform(post(BASE_URL + "/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 Bad Request khi thiếu trường bắt buộc")
        void initiateUpload_InvalidBody_MissingFields() throws Exception {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest("", "", null);

            mockMvc.perform(post(BASE_URL + "/initiate")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/user/resume-uploads/{uploadId}/complete")
    class CompleteEndpointTests {

        @Test
        @DisplayName("200 OK khi complete thành công")
        void completeUpload_Success() throws Exception {
            UUID uploadId = UUID.randomUUID();
            ResumeView view = new ResumeView(123L, "cv.pdf", LocalDateTime.now(), ResumeStatus.UPLOADED);

            when(resumeUploadService.completeUpload(eq(uploadId), any()))
                    .thenReturn(view);

            mockMvc.perform(post(BASE_URL + "/" + uploadId + "/complete")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(123))
                    .andExpect(jsonPath("$.data.fileName").value("cv.pdf"))
                    .andExpect(jsonPath("$.data.status").value("UPLOADED"));
        }

        @Test
        @DisplayName("401 Unauthorized khi chưa đăng nhập")
        void completeUpload_Unauthorized() throws Exception {
            UUID uploadId = UUID.randomUUID();

            mockMvc.perform(post(BASE_URL + "/" + uploadId + "/complete")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found khi upload session không tồn tại")
        void completeUpload_NotFound() throws Exception {
            UUID uploadId = UUID.randomUUID();
            when(resumeUploadService.completeUpload(eq(uploadId), any()))
                    .thenThrow(new ResourceNotFoundException("Upload session not found"));

            mockMvc.perform(post(BASE_URL + "/" + uploadId + "/complete")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 Forbidden khi truy cập session của user khác")
        void completeUpload_Forbidden() throws Exception {
            UUID uploadId = UUID.randomUUID();
            when(resumeUploadService.completeUpload(eq(uploadId), any()))
                    .thenThrow(new ForbiddenException("You do not have permission to view this resume."));

            mockMvc.perform(post(BASE_URL + "/" + uploadId + "/complete")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 Bad Request khi session bị hết hạn hoặc không hợp lệ")
        void completeUpload_BadRequest() throws Exception {
            UUID uploadId = UUID.randomUUID();
            when(resumeUploadService.completeUpload(eq(uploadId), any()))
                    .thenThrow(new BadRequestException("Upload session has expired"));

            mockMvc.perform(post(BASE_URL + "/" + uploadId + "/complete")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
