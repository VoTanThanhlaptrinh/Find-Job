package com.nlu.content.api;

import com.nlu.content.api.dto.BlogDTO;
import com.nlu.content.api.dto.LikeDTO;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.content.domain.model.Blog;
import com.nlu.content.domain.model.Comment;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.content.application.BlogService;
import com.nlu.shared.utils.MessageUtils;
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
        Page<Blog> blogs = blogService.getBlogs(pageIndex, pageSize);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("blog.get.success"), blogs, HttpStatus.OK.value()));
    }

    @GetMapping("/comments/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Comment>>> getListOfComments(@PathVariable int pageIndex, @PathVariable int pageSize) {
        Page<Comment> comments = blogService.getComments(pageIndex, pageSize);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("blog.get.success"), comments, HttpStatus.OK.value()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> postBlog(@RequestBody @Valid BlogDTO blogDTO, BindingResult bindingResult,
                                                        @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        String message = blogService.postBlog(blogDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(message, null, HttpStatus.CREATED.value()));
    }

    @PutMapping("/{blogId}")
    public ResponseEntity<ApiResponse<String>> updateBlog(@PathVariable long blogId,
                                                          @RequestBody @Valid BlogDTO blogDTO,
                                                          BindingResult bindingResult,
                                                          @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        String message = blogService.updateBlog(blogId, blogDTO, currentUser);
        return ResponseEntity.ok(new ApiResponse<>(message, null, HttpStatus.OK.value()));
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<ApiResponse<String>> deleteBlog(@PathVariable long blogId) {
        String message = blogService.deleteBlog(blogId);
        return ResponseEntity.ok(new ApiResponse<>(message, null, HttpStatus.OK.value()));
    }

    @GetMapping("/{blogId}")
    public ResponseEntity<ApiResponse<Blog>> getBlogDetail(@PathVariable final long blogId) {
        Blog blog = blogService.getBlogById(blogId);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("blog.get.success"), blog, HttpStatus.OK.value()));
    }

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<String>> commentBlog(@RequestBody @Valid Comment comment, BindingResult bindingResult,
                                                           @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        String message = blogService.comment(comment, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(message, null, HttpStatus.CREATED.value()));
    }

    @PostMapping("/likes")
    public ResponseEntity<ApiResponse<String>> likeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult,
                                                        @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        String message = blogService.like(likeDTO.getId(), currentUser);
        return ResponseEntity.ok(new ApiResponse<>(message, null, HttpStatus.OK.value()));
    }

    @PostMapping("/likes/remove")
    public ResponseEntity<ApiResponse<String>> unlikeBlog(@RequestBody @Valid LikeDTO likeDTO, BindingResult bindingResult,
                                                          @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        String message = blogService.unlike(likeDTO.getId(), currentUser);
        return ResponseEntity.ok(new ApiResponse<>(message, null, HttpStatus.OK.value()));
    }
}
