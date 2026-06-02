package com.job_web.identity.application;

import com.job_web.identity.api.dto.UserInfo;
import com.job_web.identity.domain.model.User;

public interface AccountService {
    UserInfo getDetailUser(User user);
    void changePassword(String newPassword, String oldPassword, User user);
    void updateInfo(UserInfo userInfo, User user);
    boolean checkOauth2(User user);
}
