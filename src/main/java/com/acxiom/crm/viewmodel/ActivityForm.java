package com.acxiom.crm.viewmodel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ActivityForm {

    private Long id;

    @NotNull
    private String type;

    @NotBlank
    private String subject;

    private String description;

    @NotNull
    private LocalDateTime activityDate;

    @NotNull
    private String status;

    private Long customerId;
    private Long leadId;
    private Long assignedToId;
}
