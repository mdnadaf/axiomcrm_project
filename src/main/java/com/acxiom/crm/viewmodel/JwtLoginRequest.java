package com.acxiom.crm.viewmodel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JwtLoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
