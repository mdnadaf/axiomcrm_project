package com.acxiom.crm.viewmodel;

import com.acxiom.crm.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterForm {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @StrongPassword
    private String password;

    @NotBlank
    private String confirmPassword;
}
