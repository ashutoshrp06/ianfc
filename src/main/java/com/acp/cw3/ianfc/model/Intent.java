package com.acp.cw3.ianfc.model;

import com.acp.cw3.ianfc.model.enums.IntentType;
import com.acp.cw3.ianfc.model.enums.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "intents")
public class Intent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "intent_id")
    private UUID intentId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "intent_type", nullable = false)
    private IntentType intentType;

    @NotBlank
    @Column(name = "target_entity", nullable = false)
    private String targetEntity;

    @Column(name = "target_region")
    private String targetRegion;

    @NotNull
    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @NotBlank
    @Column(name = "threshold_unit", nullable = false)
    private String thresholdUnit;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.MAJOR;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
