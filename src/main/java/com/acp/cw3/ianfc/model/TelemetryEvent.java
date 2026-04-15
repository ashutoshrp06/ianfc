package com.acp.cw3.ianfc.model;

import com.acp.cw3.ianfc.model.enums.EventType;
import com.acp.cw3.ianfc.model.enums.Severity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryEvent {

    private String eventId;
    private String deviceId;
    private String deviceType;
    private EventType eventType;
    private Severity severity;
    private Double metricValue;
    private String metricUnit;
    private String sourceInterface;
    private String peerDevice;
    private Instant timestamp;
    private String region;
}
