package com.acxiom.crm.viewmodel;

import com.acxiom.crm.validation.IndianPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerForm {

    /** Null for create, non-null for update */
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank
    @IndianPhone(message = "Enter a valid phone number.")
    private String phone;

    private String company;
    private String address;
    private String notes;

    private Long assignedToId;

    private boolean active;
}
