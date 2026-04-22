package web_application.admin;

import com.job_web.controller.admin.AdminDashboardController;
import com.job_web.dto.admin.dashboard.DashboardSummaryResponse;
import com.job_web.dto.admin.dashboard.PendingJobItem;
import com.job_web.dto.common.PageResponse;
import com.job_web.service.admin.AdminService;
import com.job_web.utills.MessageUtils;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, AdminDashboardController.class, MessageUtils.class})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    private static final String BASE_URL = "/admin";

    @Nested
    @DisplayName("GET /admin/dashboard/summary")
    class SummaryTests {

        @Test
        @DisplayName("AD03: Lay thong tin summary yeu cau quyen ADMIN")
        void getSummary_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/dashboard/summary"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD04: Lay thong tin summary thanh cong")
        void getSummary_Success() throws Exception {
            DashboardSummaryResponse summary = DashboardSummaryResponse.builder()
                    .totalEmployers(100)
                    .totalJobSeekers(1000)
                    .pendingJobs(10)
                    .totalRevenue(50000.0)
                    .build();

            when(adminService.getDashboardSummary()).thenReturn(summary);

            mockMvc.perform(get(BASE_URL + "/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalEmployers").value(100));
        }
    }

    @Nested
    @DisplayName("GET /admin/jobs/pending")
    class PendingJobsTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD05: Lay danh sach job cho duyet co phan trang")
        void getPendingJobs_Success() throws Exception {
            List<PendingJobItem> items = List.of(
                    PendingJobItem.builder().id("1").title("Job 1").company("Company 1").postDate(LocalDate.now()).status("pending").build()
            );
            PageResponse<PendingJobItem> response = PageResponse.<PendingJobItem>builder()
                    .items(items)
                    .pagination(PageResponse.Pagination.builder()
                            .page(1)
                            .pageSize(10)
                            .totalItems(1)
                            .totalPages(1)
                            .build())
                    .build();

            when(adminService.getPendingJobs(anyInt(), anyInt())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/jobs/pending")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].title").value("Job 1"))
                    .andExpect(jsonPath("$.data.pagination.totalItems").value(1));
        }
    }
}
