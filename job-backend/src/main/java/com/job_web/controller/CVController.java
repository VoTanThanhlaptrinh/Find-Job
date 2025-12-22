package com.job_web.controller;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.CVDTO;
import com.job_web.models.CV;
import com.job_web.service.CVService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = "/api/cv", produces = "application/json")
@AllArgsConstructor
public class CVController {
    private final CVService cvService;
    @RequestMapping("/pri/u/listCVOfUser")
    public ResponseEntity<ApiResponse<List<CVDTO>>> getListCVOfUser(Principal principal){
        ApiResponse<List<CVDTO>> res = cvService.getListCVOfUser(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    @GetMapping("/pri/h/downloadCV/{id}")
    public ResponseEntity<Resource> downloadCV(@PathVariable("id") long id){
        CV cv = cvService.findById(id);
        if(cv == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        ByteArrayResource byteArrayResource =  new ByteArrayResource(cv.getData());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(byteArrayResource);
    }
}
