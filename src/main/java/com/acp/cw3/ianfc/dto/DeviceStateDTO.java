package com.acp.cw3.ianfc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStateDTO {
    private String deviceId;
    private String linkState;
    private String bgpState;
    private String fsmState;
    private Double latencyMs;
    private Double packetLossPercent;
    private String lastUpdated;
    private String region;
}
