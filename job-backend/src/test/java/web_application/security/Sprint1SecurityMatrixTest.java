package web_application.security;

import com.nlu.admin.api.AdminCategoryController;
import com.nlu.admin.api.UserCrudController;
import com.nlu.admin.application.UserCrudService;
import com.nlu.applicationProcess.api.UserApplicationController;
import com.nlu.applicationProcess.application.JobApplicationService;
import com.nlu.recruitment.api.dto.JobDto;
import com.nlu.recruitment.application.CategoryService;
import com.nlu.recruitment.application.JobQueryService;
import com.nlu.recruitment.application.JobService;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.mapper.JobMapper;
import com.nlu.shared.domain.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserCrudController.class,
        AdminCategoryController.class,
        UserApplicationController.class
})
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class Sprint1SecurityMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserCrudService userCrudService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JobApplicationService jobApplicationService;

    @MockBean
    private JobQueryService jobQueryService;

    @MockBean
    private JobService jobService;

    @Nested
    @DisplayName("1.1 Ma trận phân quyền /api/admin/users (UserCrudController)")
    class AdminUsersSecurityTests {

        @Test
        @DisplayName("Anonymous gọi GET /api/admin/users/page -> 401")
        void anonymousCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/admin/users/page"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("USER thường gọi GET /api/admin/users/page -> 403")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void userCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/admin/users/page"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("HIRER gọi DELETE /api/admin/users/1 -> 403")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void hirerCannotDeleteUsers() throws Exception {
            mockMvc.perform(delete("/api/admin/users/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN gọi GET /api/admin/users/page -> 200")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void adminCanListUsers() throws Exception {
            when(userCrudService.getUsers(0, 10)).thenReturn(Page.empty());

            mockMvc.perform(get("/api/admin/users/page"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Đường dẫn cũ /api/users bị chặn bởi denyAll -> 403 cho USER")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void legacyApiUsersPathIsDenied() throws Exception {
            mockMvc.perform(get("/api/users/page"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("1.2 Ma trận phân quyền /api/admin/categories (AdminCategoryController)")
    class AdminCategoriesSecurityTests {

        @Test
        @DisplayName("Anonymous gọi POST /api/admin/categories -> 401")
        void anonymousCannotCreateCategory() throws Exception {
            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"IT\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("USER gọi POST /api/admin/categories -> 403")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void userCannotCreateCategory() throws Exception {
            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"IT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("HIRER gọi DELETE /api/admin/categories/1 -> 403")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void hirerCannotDeleteCategory() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN gọi DELETE /api/admin/categories/1 -> 200")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void adminCanDeleteCategory() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("1.9 Bảo vệ endpoint AI Matching /api/user/jobs/match/{cvId}")
    class JobMatchSecurityTests {

        @Test
        @DisplayName("Anonymous gọi GET /api/user/jobs/match/1 -> 401")
        void anonymousCannotMatchJobs() throws Exception {
            mockMvc.perform(get("/api/user/jobs/match/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("HIRER gọi GET /api/user/jobs/match/1 -> 403")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void hirerCannotMatchJobs() throws Exception {
            mockMvc.perform(get("/api/user/jobs/match/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER gọi GET /api/user/jobs/match/99 với CV không thuộc mình -> 403")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void userCannotMatchJobsWithUnownedCv() throws Exception {
            when(jobService.matchJobs(eq(99L), any()))
                    .thenThrow(new ForbiddenException("You do not have permission to access this resume"));

            mockMvc.perform(get("/api/user/jobs/match/99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER gọi GET /api/user/jobs/match/1 với CV của mình -> 200")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void userCanMatchJobsWithOwnCv() throws Exception {
            when(jobService.matchJobs(eq(1L), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/user/jobs/match/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("1.4 Chống Stored XSS khi map JobDto sang Job")
    class JobXssSanitizationTests {

        @Test
        @DisplayName("JobMapper loại bỏ thẻ <script> và thuộc tính onerror khỏi mô tả công việc")
        void jobMapperSanitizesDangerousHtml() {
            JobMapper mapper = new JobMapper();
            JobDto dto = new JobDto(
                    "Backend Engineer",
                    1L,
                    EmploymentType.FULL_TIME,
                    "$2000",
                    "<p>Valid text</p><img src=x onerror=alert(1)><script>stealToken()</script>",
                    "<ul><li>Java 17</li></ul><svg onload=alert(2)>",
                    "Java, Spring",
                    LocalDate.now().plusDays(10),
                    "<a href=\"javascript:alert(3)\">Click</a>",
                    2,
                    false,
                    1L
            );

            Job job = mapper.toJob(dto);

            assertTrue(job.getDescription().contains("<p>Valid text</p>"));
            assertFalse(job.getDescription().contains("<script>"));
            assertFalse(job.getDescription().contains("onerror"));
            assertFalse(job.getRequireDetails().contains("onload"));
            assertFalse(job.getMoreDetail().contains("javascript:"));
        }
    }
}

