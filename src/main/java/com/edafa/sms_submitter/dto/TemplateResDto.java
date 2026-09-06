package com.edafa.sms_submitter.dto;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResDto {
    private Integer templateId;
    private String name;
    private String body;
    private Integer companyId;
    private String companyName;
}