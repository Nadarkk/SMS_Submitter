package com.edafa.sms_submitter.dto;

import com.edafa.sms_submitter.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResDto {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String phoneNo;
    private Boolean isActive;
    private Role role;
    private Integer companyId;
    private String companyName;
}