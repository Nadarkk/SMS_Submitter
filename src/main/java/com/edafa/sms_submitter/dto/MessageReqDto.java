package com.edafa.sms_submitter.dto;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageReqDto {
    @NotEmpty(message = "At least one contact is required")
    private List<Integer> contactIds;

    @NotBlank(message = "SMS body text is required")
    @Size(max = 1600, message = "SMS content exceeds character limit (1600 Character)")
    private String body;

    @NotBlank(message = "Sender Name is required")
    @Size(max = 50, message = "Sender Name exceeds character limit")
    private String sender;

    @Positive
    private Integer templateId; // Optional
}