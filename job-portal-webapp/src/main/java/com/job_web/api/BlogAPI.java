package com.job_web.api;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.BlogDTO;
import com.job_web.dto.LikeDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import com.job_web.service.BlogService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/api/blog")
@CrossOrigin("**")
@AllArgsConstructor
public class BlogAPI {
    private final BlogService blogService;

    @GetMapping("/pub/blogList/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Blog>>> getListOfBlogs(@PathVariable int pageIndex, @PathVariable int pageSize) {
        ApiResponse<Page<Blog>> res = blogService.getBlogs(pageIndex,pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pub/commentList/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Comment>>> getListOfComments(@PathVariable int pageIndex, @PathVariable int pageSize) {
        ApiResponse<Page<Comment>> res = blogService.getComments(pageIndex,pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pri/postBlog")
    public ResponseEntity<ApiResponse<String>> postBlog(@RequestBody @Valid BlogDTO blogDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage()
                    ,null,HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.postBlog(blogDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    @GetMapping("/pub/blogDetail/{blogId}")
    public ResponseEntity<ApiResponse<Blog>> getListOfBlogs(@PathVariable final long blogId) {
        ApiResponse<Blog> res = blogService.getBlogById(blogId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pri/comment")
    public ResponseEntity<ApiResponse<String>> commentBlog(@RequestBody @Valid Comment comment, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage()
                            ,null,HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.comment(comment);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pri/like")
    public ResponseEntity<ApiResponse<String>> likeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage()
                            ,null,HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.like(likeDTO.getId());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pri/unlike")
    public ResponseEntity<ApiResponse<String>> unlikeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage()
                            ,null,HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.unlike(likeDTO.getId());
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
