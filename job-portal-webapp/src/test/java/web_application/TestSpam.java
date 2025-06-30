package web_application;

import com.job_web.dto.LoginDTO;
import com.job_web.service.AccountService;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;

@AutoConfigureMockMvc
@SpringBootTest
@AllArgsConstructor
public class TestSpam {
    private MockMvc mockMvc;
    private AccountService service;
    @Test
    public void test() throws Exception {
        LoginDTO dto = new LoginDTO("vtthanh32004@gmail.com","123");
        ArrayList<LoginDTO> dtos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            dtos.add(dto);
        }
        mockMvc.perform(MockMvcRequestBuilders.post("api/account/pub/login"));
    }
}
