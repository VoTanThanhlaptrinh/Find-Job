package web_application.job;

import com.job_web.controller.job.HirerJobController;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobDTO;
import com.job_web.service.job.JobQueryService;
import com.job_web.service.job.JobService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HirerJobController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class HirerJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private JobQueryService jobQueryService;

    private static final String BASE_URL = "/api/hirer/jobs";

    @Nested
    @DisplayName("POST /api/hirer/jobs - Dang tin tuyen dung")
    class CreateJobTests {

        @Test
        @DisplayName("HJ01: Tao job thanh cong voi quyen HIRER")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void createJob_Success() throws Exception {
            org.mockito.Mockito.doNothing().when(jobService).createJob(any(JobDTO.class), any());

            mockMvc.perform(multipart(BASE_URL)
                            .param("jobName", "Java Developer")
                            .param("addressId", "1")
                            .param("jobType", "Full-time")
                            .param("salary", "1000-1500")
                            .param("jobDescription", "Build backend APIs")
                            .param("jobRequirement", "2 years experience")
                            .param("jobSkill", "Java, Spring Boot")
                            .param("deadlineCV", "2099-12-31")
                            .param("hirerId", "1")
                            .param("moreDetail", "Remote 2 days/week"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Tao job thanh cong"));
        }

        @Test
        @DisplayName("HJ02: Tra ve 400 khi jobName rong")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void createJob_JobNameBlank() throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .param("jobName", "")
                            .param("addressId", "1")
                            .param("jobType", "Full-time")
                            .param("salary", "1000-1500")
                            .param("jobDescription", "Build backend APIs")
                            .param("jobRequirement", "2 years experience")
                            .param("jobSkill", "Java, Spring Boot")
                            .param("deadlineCV", "2099-12-31")
                            .param("hirerId", "1")
                            .param("moreDetail", "Remote 2 days/week"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("HJ03: Tra ve 401 khi chua dang nhap")
        void createJob_Unauthorized() throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .param("jobName", "Java Developer"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("HJ04: Tra ve 403 khi USER dang job cua HIRER")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void createJob_ForbiddenForUser() throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .param("jobName", "Java Developer"))
                    .andExpect(status().isForbidden());
        }
    }
}
