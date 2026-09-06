package com.edafa.sms_submitter.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResDto {
    private Integer messageId;
    private Instant sendDate;
    private String sender;
    private String body;
    private Integer segmentCount=1;
    private Integer userId;
    private String firstName;
    private String lastName;
    private Integer templateId;
    private String templateName;
    private List<SmsResDto> recipients; // contacts sent to + status
}