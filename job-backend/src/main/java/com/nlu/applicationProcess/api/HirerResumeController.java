package com.nlu.applicationProcess.api;

import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.applicationProcess.application.ResumeService;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/hirer/resumes", produces = "application/json")
@PreAuthorize("hasAnyAuthority('ROLE_HIRER', 'HIRER')")
@RequiredArgsConstructor
public class HirerResumeController {
    private final ResumeService resumeService;

    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeViewUrl(@PathVariable("id") long id,
                                                                      @CurrentUser User currentUser) {
        ResumeUrlDTO res = resumeService.getResumeViewUrlForHirer(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeDownloadUrl(@PathVariable("id") long id,
                                                                          @CurrentUser User currentUser) {
        ResumeUrlDTO res = resumeService.getResumeDownloadUrlForHirer(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }
}
