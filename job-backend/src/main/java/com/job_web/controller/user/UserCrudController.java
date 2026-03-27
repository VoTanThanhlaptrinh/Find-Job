package com.job_web.controller.user;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import com.job_web.service.user.UserCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.security.Principal;

@RestController
@RequestMapping(path = "/api/users", produces = "application/json")
@RequiredArgsConstructor
public class UserCrudController {
    private final UserCrudService userCrudService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody UserCrudDTO userDTO,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = userCrudService.createUser(userDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/page/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> list(@PathVariable("pageIndex") int pageIndex,
                                                                   @PathVariable("pageSize") int pageSize) {
        ApiResponse<Page<UserResponseDTO>> res = userCrudService.getUsers(pageIndex, pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> detail(Principal principal) {
        if(principal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("",null, HttpStatus.UNAUTHORIZED.value()));
        }
        ApiResponse<UserResponseDTO> res = userCrudService.getUserByEmail(principal.getName());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> update(@PathVariable("id") long id,
                                                      @Valid @RequestBody UserCrudDTO userDTO,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = userCrudService.updateUser(id, userDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable("id") long id) {
        ApiResponse<String> res = userCrudService.deleteUser(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
