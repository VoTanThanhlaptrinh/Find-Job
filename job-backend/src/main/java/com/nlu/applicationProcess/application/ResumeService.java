package com.nlu.applicationProcess.application;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeDetailDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.identity.domain.model.User;

import java.util.List;

public interface ResumeService {
    List<ResumeView> getListResumeOfUser(User user);

    ResumeDetailDTO getResumeDetail(long id, User user);

    ResumeView createResume(ResumeUploadDTO resumeUploadDTO, User user);

    void deleteResume(long id, User user);

    ResumeUrlDTO getResumeViewUrl(long id, User user);

    ResumeUrlDTO getResumeDownloadUrl(long id, User user);

    ResumeUrlDTO getResumeViewUrlForHirer(long id);

    ResumeUrlDTO getResumeDownloadUrlForHirer(long id);
}
