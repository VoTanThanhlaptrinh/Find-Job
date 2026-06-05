package com.nlu.identity.application;

import com.nlu.identity.api.dto.UserInfo;
import com.nlu.identity.domain.model.User;

public interface AccountService {
    UserInfo getDetailUser(User user);
    void changePassword(String newPassword, String oldPassword, User user);
    void updateInfo(UserInfo userInfo, User user);
    boolean checkOauth2(User user);
}
