package com.job_web.identity.mapper;

import com.job_web.identity.domain.vo.PhoneNumber;
import com.job_web.identity.api.dto.UserInfo;
import com.job_web.identity.domain.model.User;
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
        user.setMobile(new PhoneNumber(userInfo.getMobile()));
    }
}
