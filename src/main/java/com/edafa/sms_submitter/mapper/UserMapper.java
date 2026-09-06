package com.edafa.sms_submitter.mapper;

import com.edafa.sms_submitter.dto.UserResDto;
import org.springframework.stereotype.Component;
import com.edafa.sms_submitter.dto.UserReqDto;
import com.edafa.sms_submitter.entity.*;

@Component
public class UserMapper {
    public User toEntity(UserReqDto dto, Company company, String encodedPassword) {
        if (dto == null) return null;

        return new User(dto.getFirstName(),dto.getLastName(), dto.getPhoneNo(), encodedPassword,dto.getRole(), company);
    }

    public UserResDto toDto(User user){
        if (user ==null) return null;

        return new UserResDto(user.getUserId(), user.getFirstName(), user.getLastName(), user.getPhoneNo(), user.getIsActive(),user.getRole(),
                user.getCompany() != null? user.getCompany().getCompanyId() : null,
                user.getCompany() != null? user.getCompany().getCompanyName() : null);
    }
}