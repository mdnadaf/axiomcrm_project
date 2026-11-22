package com.acxiom.crm.viewmodel;

import com.acxiom.crm.validation.IndianPhone;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LeadForm {

    private Long id;

    @NotBlank
    private String name;

    private String company;

    @NotBlank
    @Email
    private String email;

    @IndianPhone
    private String phone;

    @NotNull
    private String status;

    private String source;
    private String notes;

    @DecimalMin("0")
    private BigDecimal expectedValue;

    private LocalDate expectedCloseDate;
    private Long customerId;
    private Long assignedToId;
}
