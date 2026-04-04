package web_application.home;

import com.job_web.controller.home.HomeFeedController;
import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.job.JobCardView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeFeedController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class HomeFeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private BlogRepository blogRepository;

    private static final String BASE_URL = "/api/home";

    @Nested
    @DisplayName("GET /api/home/init - Lay du lieu trang chu")
    class InitTests {

        @Test
        @DisplayName("HM01: Anonymous van lay duoc feed trang chu")
        void getInit_Success() throws Exception {
            List<JobCardView> jobs = List.of(
                    new JobCardView(1L, "Java Developer", "Ho Chi Minh", "1000-1500", "Full-time")
            );
            when(jobRepository.findJobs(any(LocalDateTime.class), eq("ACTIVE"), any(Pageable.class))).thenReturn(jobs);

            mockMvc.perform(get(BASE_URL + "/init")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Java Developer"))
                    .andExpect(jsonPath("$.status").value(200));
        }

        @Test
        @DisplayName("HM02: Tra ve 405 khi goi sai method")
        void getInit_InvalidMethod() throws Exception {
            mockMvc.perform(post(BASE_URL + "/init"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("HM03: Endpoint /api/home cho phep anonymous truy cap")
        void home_PublicEndpoint() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Hello world"));
        }
    }
}
