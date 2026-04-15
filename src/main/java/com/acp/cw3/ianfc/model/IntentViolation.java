package com.acp.cw3.ianfc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intent_violations")
public class IntentViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "violation_id")
    private UUID violationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intent_id", nullable = false)
    private Intent intent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fault_id")
    private CorrelatedFault fault;

    @Column(name = "intent_name")
    private String intentName;

    @Column(name = "observed_value")
    private Double observedValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "threshold_unit")
    private String thresholdUnit;

    @Column(name = "violated_at")
    private Instant violatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    public void prePersist() {
        if (violatedAt == null) {
            violatedAt = Instant.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
