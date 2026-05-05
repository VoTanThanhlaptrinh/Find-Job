package web_application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.controller.admin.AdminJobsController;
import com.job_web.dto.admin.job.BulkActionRequest;
import com.job_web.dto.admin.job.JobMetricsResponse;
import com.job_web.service.admin.AdminService;
import com.job_web.utils.MessageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminJobsController.class)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, AdminJobsController.class, MessageUtils.class})
class AdminJobsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    private static final String BASE_URL = "/admin/jobs";

    @Nested
    @DisplayName("GET /admin/jobs/metrics")
    class MetricsTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD09: Lay chi so do luong cong viec")
        void getMetrics_Success() throws Exception {
            JobMetricsResponse metrics = JobMetricsResponse.builder()
                    .livePostings(50)
                    .pendingReview(5)
                    .totalApplicants(500)
                    .build();

            when(adminService.getJobMetrics()).thenReturn(metrics);

            mockMvc.perform(get(BASE_URL + "/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.livePostings").value(50));
        }
    }

    @Nested
    @DisplayName("POST /admin/jobs/bulk-action")
    class BulkActionTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD10: Thuc hien thao tac hang loat tren nhieu job")
        void bulkAction_Success() throws Exception {
            BulkActionRequest request = new BulkActionRequest(List.of("1", "2"), "archive");
            
            doNothing().when(adminService).bulkJobAction(any(BulkActionRequest.class));

            mockMvc.perform(post(BASE_URL + "/bulk-action")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.processed").value(2));
        }
    }
}
