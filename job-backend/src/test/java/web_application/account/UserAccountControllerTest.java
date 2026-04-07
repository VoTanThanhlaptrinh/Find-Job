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
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("UA01: USER truy cap endpoint kiem tra role thanh cong")
    void checkUserLogin_Success() throws Exception {
        User user = buildUser("user@test.com", "ROLE_USER");

        mockMvc.perform(get("/api/account/roles/user")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("UA02: Anonymous bi tu choi voi 401")
    void checkUserLogin_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/account/roles/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UA03: HIRER khong duoc truy cap endpoint cua USER")
    void checkUserLogin_ForbiddenForHirer() throws Exception {
        User hirer = buildUser("hirer@test.com", "ROLE_HIRER");

        mockMvc.perform(get("/api/account/roles/user")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                hirer,
                                null,
                                hirer.getAuthorities()
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
