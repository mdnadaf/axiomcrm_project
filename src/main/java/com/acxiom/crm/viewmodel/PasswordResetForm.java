package com.acxiom.crm.viewmodel;

import com.acxiom.crm.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PasswordResetForm {

    @StrongPassword
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
