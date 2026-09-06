package com.edafa.sms_submitter.dto;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactResDto {
    private Integer contactId;
    private String name;
    private String phoneNo;
    private Integer companyId;
    private String companyName;
}