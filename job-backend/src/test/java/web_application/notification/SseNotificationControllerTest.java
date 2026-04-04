package web_application.notification;

import com.job_web.controller.notification.SseNotificationController;
import com.job_web.service.notification.SseNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SseNotificationController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class SseNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseNotificationService sseNotificationService;

    private static final String BASE_URL = "/api/notifications";

    @Nested
    @DisplayName("POST /api/notifications/{resumeId}/send - Gui thong bao")
    class SendNotificationTests {

        @Test
        @DisplayName("SN01: Gui thong bao thanh cong khi da dang nhap")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void sendNotification_Success() throws Exception {
            doNothing().when(sseNotificationService).sendNotification(1L, "Interview invitation");

            mockMvc.perform(post(BASE_URL + "/1/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("Interview invitation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification sent"))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()));
        }

        @Test
        @DisplayName("SN02: Tra ve 400 khi resumeId khong hop le")
        @WithMockUser(username = "user@test.com", roles = "USER")
        void sendNotification_InvalidResumeId() throws Exception {
            mockMvc.perform(post(BASE_URL + "/invalid/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("Interview invitation"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("SN03: Tra ve 401 khi anonymous gui thong bao")
        void sendNotification_Unauthorized() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("Interview invitation"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
