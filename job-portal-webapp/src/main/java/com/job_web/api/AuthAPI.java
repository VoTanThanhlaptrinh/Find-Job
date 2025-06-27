package com.job_web.api;

import com.job_web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RequestMapping(path = "/auth", produces = "application/json")
@CrossOrigin(origins = "**")
@RestController
public class AuthAPI {
    @GetMapping("/url/google")
    public ResponseEntity<ApiResponse<String>> googleUrl(HttpServletRequest req) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String url = base + "/oauth2/authorization/google";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<String>("success",url,HttpStatus.OK.value()));
    }
}
