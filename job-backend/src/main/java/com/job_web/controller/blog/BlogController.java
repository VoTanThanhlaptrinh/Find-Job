package com.job_web.controller.blog;

import com.job_web.dto.blog.BlogDTO;
import com.job_web.dto.blog.LikeDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import com.job_web.service.blog.BlogService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/blogs")
@AllArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @GetMapping("/page/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Blog>>> getListOfBlogs(@PathVariable int pageIndex, @PathVariable int pageSize) {
        ApiResponse<Page<Blog>> res = blogService.getBlogs(pageIndex, pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/comments/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Comment>>> getListOfComments(@PathVariable int pageIndex, @PathVariable int pageSize) {
        ApiResponse<Page<Comment>> res = blogService.getComments(pageIndex, pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> postBlog(@RequestBody @Valid BlogDTO blogDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.postBlog(blogDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/{blogId}")
    public ResponseEntity<ApiResponse<String>> updateBlog(@PathVariable long blogId,
                                                          @RequestBody @Valid BlogDTO blogDTO,
                                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.updateBlog(blogId, blogDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<ApiResponse<String>> deleteBlog(@PathVariable long blogId) {
        ApiResponse<String> res = blogService.deleteBlog(blogId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/{blogId}")
    public ResponseEntity<ApiResponse<Blog>> getBlogDetail(@PathVariable final long blogId) {
        ApiResponse<Blog> res = blogService.getBlogById(blogId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<String>> commentBlog(@RequestBody @Valid Comment comment, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.comment(comment);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/likes")
    public ResponseEntity<ApiResponse<String>> likeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.like(likeDTO.getId());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/likes/remove")
    public ResponseEntity<ApiResponse<String>> unlikeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = blogService.unlike(likeDTO.getId());
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
