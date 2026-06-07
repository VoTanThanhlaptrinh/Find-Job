package web_application.notification;

import com.nlu.shared.api.SseNotificationController;
import com.nlu.shared.application.SseEmitterService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import web_application.support.TestSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SseNotificationController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class SseNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseEmitterService sseEmitterService;

    private static final String CONNECT_URL = "/api/sse/connect";

    @Nested
    @DisplayName("GET /api/sse/connect - SSE Connection")
    class ConnectTests {

        @Test
        @DisplayName("Should return 200 with SSE stream when authenticated")
        @WithMockUser(username = "user@test.com", roles = {"USER"})
        void shouldReturnSseStream_whenAuthenticated() throws Exception {
            SseEmitter emitter = new SseEmitter();
            when(sseEmitterService.createEmitter(any())).thenReturn(emitter);

            mockMvc.perform(get(CONNECT_URL)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401_whenNotAuthenticated() throws Exception {
            mockMvc.perform(get(CONNECT_URL)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }
}
