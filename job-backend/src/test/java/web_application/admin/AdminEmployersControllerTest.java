package web_application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.controller.admin.AdminEmployersController;
import com.job_web.dto.admin.employer.EmployerDetail;
import com.job_web.dto.admin.employer.EmployerListItem;
import com.job_web.dto.admin.employer.EmployerStatusRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEmployersController.class)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, AdminEmployersController.class, MessageUtils.class})
class AdminEmployersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    private static final String BASE_URL = "/admin/employers";

    @Nested
    @DisplayName("GET /admin/employers")
    class ListTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD06: Lay danh sach nha tuyen dung voi filter search")
        void getEmployers_WithSearch() throws Exception {
            List<EmployerListItem> items = List.of(
                    EmployerListItem.builder().id("1").name("Tech Corp").industry("IT").registrationDate(LocalDate.now()).accountStatus("active").build()
            );
            PageResponse<EmployerListItem> response = PageResponse.<EmployerListItem>builder()
                    .items(items)
                    .pagination(PageResponse.Pagination.builder().page(1).pageSize(10).totalItems(1).totalPages(1).build())
                    .build();

            when(adminService.getEmployers(anyInt(), anyInt(), anyString(), any(), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .param("search", "Tech")
                            .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].name").value("Tech Corp"));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/employers/{id}/status")
    class StatusTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD07: Cap nhat trang thai nha tuyen dung (Suspend)")
        void updateStatus_Success() throws Exception {
            EmployerStatusRequest request = new EmployerStatusRequest("suspend", "Policy violation");
            
            doNothing().when(adminService).updateEmployerStatus(eq(1L), any(EmployerStatusRequest.class));

            mockMvc.perform(patch(BASE_URL + "/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updated").value(true));
        }
    }

    @Nested
    @DisplayName("GET /admin/employers/{id}")
    class DetailTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("AD08: Lay thong tin chi tiet nha tuyen dung")
        void getDetail_Success() throws Exception {
            EmployerDetail detail = EmployerDetail.builder()
                    .id("1")
                    .name("Tech Corp")
                    .contactEmail("hr@techcorp.com")
                    .accountStatus("active")
                    .registrationDate(LocalDate.now())
                    .build();

            when(adminService.getEmployerDetail(1L)).thenReturn(detail);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contactEmail").value("hr@techcorp.com"));
        }
    }
}
