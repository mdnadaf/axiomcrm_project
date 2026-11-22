package com.acxiom.crm.viewmodel;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class OpportunityForm {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Long customerId;

    private Long leadId;

    @NotNull
    private String stage;

    @NotNull
    @DecimalMin(value = "0.01", message = "Opportunity Amount must be greater than 0.")
    private BigDecimal amount;

    @NotNull
    @Min(value = 0, message = "Probability must be between 0 and 100.")
    @Max(value = 100, message = "Probability must be between 0 and 100.")
    private Integer probability;

    private LocalDate expectedCloseDate;
    private String description;
    private Long assignedToId;
}
