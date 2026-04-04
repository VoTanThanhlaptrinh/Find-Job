package com.job_web.service.account;

import com.job_web.dto.profile.UserInfo;
import com.job_web.models.User;

public interface AccountService {
    UserInfo getDetailUser(User user);
    void changePassword(String newPassword, String oldPassword, User user);
    void updateInfo(UserInfo userInfo, User user);
    boolean checkOauth2(User user);
}
