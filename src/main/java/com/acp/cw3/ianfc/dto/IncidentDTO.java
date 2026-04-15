package com.acp.cw3.ianfc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDTO {
    private UUID faultId;
    private String rootCauseDeviceId;
    private String rootCauseEventType;
    private UUID rootCauseEventId;
    private List<String> affectedDevices;
    private Integer suppressedAlarmCount;
    private Long correlationWindowMs;
    private Instant detectedAt;
    private Instant resolvedAt;
    private String status;
}
