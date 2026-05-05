package com.job_web.mapper;

import com.job_web.dto.profile.UserInfo;
import com.job_web.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return new UserInfo(
                user.getFullName(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getMobile()
        );
    }

    public void updateUser(UserInfo userInfo, User user) {
        if (userInfo == null || user == null) {
            return;
        }
        user.setFullName(userInfo.getFullName());
        user.setDateOfBirth(userInfo.getDateOfBirth());
        user.setAddress(userInfo.getAddress());
        user.setMobile(new com.job_web.models.vo.PhoneNumber(userInfo.getMobile()));
    }
}
