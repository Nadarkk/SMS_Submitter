package com.edafa.sms_submitter.dto;

import com.edafa.sms_submitter.entity.SmsStatus;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmsDetailResDto {
    private Integer smsId;
    private String simId;
    private SmsStatus status;
    private Integer messageId; // from message
    private String body;     // from message
    private Instant sendDate;   //from message
    private Integer contactId; //from contact
    private String contactName; //from contact
    private String destinationPhone;  //from contact

}