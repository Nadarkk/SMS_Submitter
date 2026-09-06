package com.edafa.sms_submitter.dto;
import com.edafa.sms_submitter.entity.Role;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReqDto {

    @NotBlank(message = "First name is required")
    @Size(min=3, max = 50, message = "First name must be 3-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min=3, max = 50, message = "Last name must be 3-50 characters")
    private String lastName;

    @NotBlank(message = "Phone Number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid Phone number")
    private String phoneNo;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password should be 8-50 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Password must contain one digit, one lowercase, one uppercase, and one special character.")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotNull(message = "Company ID is required")
    private Integer companyId;
}
