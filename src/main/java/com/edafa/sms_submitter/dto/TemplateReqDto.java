package com.edafa.sms_submitter.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateReqDto {

    @NotBlank(message = "Template name is required")
    @Size(max = 50, message = "Template name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Template body is required")
    @Size(max = 5000, message = "Message body must not exceed 5000 characters")
    private String body;


}