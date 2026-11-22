package com.acxiom.crm.entity;

import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities",
    indexes = {
        @Index(name = "idx_opp_stage", columnList = "stage"),
        @Index(name = "idx_opp_status", columnList = "status"),
        @Index(name = "idx_opp_customer", columnList = "customer_id"),
        @Index(name = "idx_opp_assigned", columnList = "assigned_to_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStage stage = OpportunityStage.QUALIFICATION;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStatus status = OpportunityStatus.OPEN;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Integer probability = 0;

    @Column(precision = 15, scale = 2)
    private BigDecimal weightedValue;

    private LocalDate expectedCloseDate;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    public void deriveFields() {
        if (stage == OpportunityStage.WON) {
            this.status = OpportunityStatus.WON;
        } else if (stage == OpportunityStage.LOST) {
            this.status = OpportunityStatus.LOST;
        } else {
            this.status = OpportunityStatus.OPEN;
        }
        if (amount != null && probability != null) {
            this.weightedValue = amount.multiply(BigDecimal.valueOf(probability))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }
}
