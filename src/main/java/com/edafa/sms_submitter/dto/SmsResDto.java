package com.edafa.sms_submitter.dto;
import com.edafa.sms_submitter.entity.SmsStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmsResDto {
    private Integer smsId;
    private String simId;
    private Integer contactId;
    private String contactName;
    private String contactPhone;
    private SmsStatus status;
}