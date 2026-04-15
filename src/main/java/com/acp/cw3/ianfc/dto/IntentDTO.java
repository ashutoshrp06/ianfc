package com.acp.cw3.ianfc.dto;

import com.acp.cw3.ianfc.model.enums.IntentType;
import com.acp.cw3.ianfc.model.enums.Severity;
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
public class IntentDTO {
    private UUID intentId;

    @NotBlank
    private String name;

    @NotNull
    private IntentType intentType;

    @NotBlank
    private String targetEntity;

    private String targetRegion;

    @NotNull
    private Double thresholdValue;

    @NotBlank
    private String thresholdUnit;

    private Severity severity;
    private Boolean active;
    private Instant createdAt;
}
