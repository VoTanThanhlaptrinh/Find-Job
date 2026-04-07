package web_application.account;

import com.job_web.JobPortalWebApplication;
import com.job_web.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class HirerAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("HA01: HIRER truy cap endpoint kiem tra role thanh cong")
    void checkHirerLogin_Success() throws Exception {
        User hirer = buildUser("hirer@test.com", "ROLE_HIRER");

        mockMvc.perform(get("/api/account/roles/hirer")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                hirer,
                                null,
                                hirer.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("HA02: Anonymous bi tu choi voi 401")
    void checkHirerLogin_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/account/roles/hirer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HA03: USER khong duoc truy cap endpoint cua HIRER")
    void checkHirerLogin_ForbiddenForUser() throws Exception {
        User user = buildUser("user@test.com", "ROLE_USER");

        mockMvc.perform(get("/api/account/roles/hirer")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        ))))
                .andExpect(status().isForbidden());
    }

    private User buildUser(String email, String role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
