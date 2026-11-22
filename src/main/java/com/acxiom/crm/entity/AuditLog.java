package com.acxiom.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "userId"),
        @Index(name = "idx_audit_entity", columnList = "entityName"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_date", columnList = "createdDate")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Long userId;

    @Column(updatable = false)
    private String username;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(nullable = false, updatable = false)
    private String entityName;

    @Column(updatable = false)
    private Long recordId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(updatable = false)
    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;
}
