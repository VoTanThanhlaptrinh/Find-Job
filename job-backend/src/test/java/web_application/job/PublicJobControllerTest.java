package web_application.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.controller.job.PublicJobController;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobFilterDTO;
import com.job_web.service.ai.ApiService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicJobController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class PublicJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private JobQueryService jobQueryService;

    @MockBean
    private ApiService apiService;

    private static final String BASE_URL = "/api/jobs";

    @Nested
    @DisplayName("GET /api/jobs/newest/{pageIndex}/{pageSize} - Lay job moi nhat")
    class GetNewestJobTests {

        @Test
        @DisplayName("PJ01: Anonymous van xem duoc danh sach job public")
        void getNewestJobs_Success() throws Exception {
            Page<JobCardView> page = new PageImpl<>(List.of(
                    new JobCardView(1L, "Java Developer", "Da Nang", "1000-1500", "Full-time")
            ));
            ApiResponse<Page<JobCardView>> response = new ApiResponse<>("success", page, HttpStatus.OK.value());
            when(jobQueryService.getListJobNewest(eq(0), eq(10))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/newest/0/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Java Developer"));
        }
    }

    @Nested
    @DisplayName("POST /api/jobs/filter - Loc job public")
    class FilterJobTests {

        @Test
        @DisplayName("PJ02: Anonymous van goi duoc filter public")
        void filterJobs_PublicEndpoint() throws Exception {
            JobFilterDTO dto = new JobFilterDTO(
                    0,
                    10,
                    1000,
                    2000,
                    List.of("Ho Chi Minh"),
                    List.of("Full-time"),
                    "Java"
            );
            ApiResponse<Page<JobCardView>> response = new ApiResponse<>("success", Page.empty(), HttpStatus.OK.value());
            when(jobQueryService.filterBetterSalaryAndHasAddressAndInTimes(
                    eq(0), eq(10), eq(1000), eq(2000),
                    eq(List.of("Ho Chi Minh")), eq(List.of("Full-time")), eq("Java")
            )).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("PJ03: Tra ve 400 khi cvId khong hop le")
        void getMatchedJobs_InvalidCvId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/match/not-a-number"))
                    .andExpect(status().isBadRequest());
        }
    }
}
