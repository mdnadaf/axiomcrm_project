package com.acxiom.crm.viewmodel;

import com.acxiom.crm.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserForm {

    private Long id;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    /** Used only for create / password reset; may be null on plain profile updates. */
    @StrongPassword
    private String password;

    @NotNull
    private String role;

    private boolean active;
}
