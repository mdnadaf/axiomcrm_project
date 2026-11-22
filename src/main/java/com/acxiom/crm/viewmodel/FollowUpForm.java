package com.acxiom.crm.viewmodel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class FollowUpForm {

    private Long id;

    @NotNull
    private LocalDate followUpDate;

    @NotNull
    private String type;

    private String remarks;

    @NotNull
    private String status;

    private Long customerId;
    private Long leadId;
    private Long assignedToId;
    private LocalDate rescheduledDate;
    private String completionNotes;
}
