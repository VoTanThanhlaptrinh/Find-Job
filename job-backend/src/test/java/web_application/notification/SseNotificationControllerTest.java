package web_application.notification;

import com.nlu.shared.api.SseNotificationController;
import com.nlu.shared.application.SseNotificationService;
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


    }
}
