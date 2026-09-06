package com.edafa.sms_submitter.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddContactReqDto {
    @NotBlank (message = "Name is required")
    @Size(min=3, max=50, message = "Name size is not valid ")
    private String name;

    @NotBlank(message = "Phone Number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNo;

}
