package web_application.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.content.api.BlogController;
import com.nlu.content.api.dto.BlogDTO;
import com.nlu.content.domain.model.Blog;
import com.nlu.content.application.BlogService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlogController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlogService blogService;

    private static final String BASE_URL = "/api/blogs";

    @Nested
    @DisplayName("GET /api/blogs/{blogId} - Xem chi tiet blog")
    class GetBlogDetailTests {

        @Test
        @DisplayName("BL01: Anonymous van xem duoc blog public")
        void getBlogDetail_Success() throws Exception {
            Blog blog = new Blog();
            blog.setId(1L);
            blog.setTitle("Spring Boot Testing");
            blog.setDescription("Guide");
            blog.setContent("Noi dung blog");
            blog.setAmountLike(10);
            when(blogService.getBlogById(eq(1L))).thenReturn(blog);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Spring Boot Testing"))
                    .andExpect(jsonPath("$.data.amountLike").value(10));
        }
    }

    @Nested
    @DisplayName("POST /api/blogs - Dang blog")
    class PostBlogTests {

        @Test
        @DisplayName("BL02: Tra ve 401 khi anonymous tao blog")
        void postBlog_Unauthorized() throws Exception {
            BlogDTO dto = new BlogDTO("Tieu de", "Mo ta", "Noi dung");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("BL03: Tra ve 400 khi title rong")
        @WithMockUser(username = "writer@test.com", roles = "USER")
        void postBlog_TitleBlank() throws Exception {
            BlogDTO dto = new BlogDTO("", "Mo ta", "Noi dung");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
