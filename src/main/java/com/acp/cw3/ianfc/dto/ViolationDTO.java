package com.acp.cw3.ianfc.dto;

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
public class ViolationDTO {
    private UUID violationId;
    private UUID intentId;
    private UUID faultId;
    private String intentName;
    private Double observedValue;
    private Double thresholdValue;
    private String thresholdUnit;
    private Instant violatedAt;
    private Instant resolvedAt;
    private String status;
}
