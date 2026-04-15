package com.acp.cw3.ianfc.service;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.Intent;
import com.acp.cw3.ianfc.model.enums.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DEVICE_STATE_KEY = "device:state:";

    /**
     * BGP_ADJACENCY      - any correlated fault (LINK_DOWN or BGP_SESSION_DOWN) on targetEntity
     *                      threatens BGP adjacency - physical link down also drops BGP
     * MIN_LINK_AVAILABILITY - LINK_DOWN on targetEntity
     * MAX_LATENCY        - LATENCY_SPIKE on targetEntity
     * MAX_PACKET_LOSS    - PACKET_LOSS on targetEntity
     */
    public boolean isViolated(Intent intent, CorrelatedFault fault) {
        if (!intent.getTargetEntity().equals(fault.getRootCauseDeviceId())) {
            return false;
        }
        String eventType = fault.getRootCauseEventType();
        return switch (intent.getIntentType()) {
            case BGP_ADJACENCY         -> "BGP_SESSION_DOWN".equals(eventType) || "LINK_DOWN".equals(eventType);
            case MIN_LINK_AVAILABILITY -> "LINK_DOWN".equals(eventType);
            case MAX_LATENCY           -> "LATENCY_SPIKE".equals(eventType);
            case MAX_PACKET_LOSS       -> "PACKET_LOSS".equals(eventType);
        };
    }

    public double extractObservedValue(Intent intent, CorrelatedFault fault) {
        String deviceKey = DEVICE_STATE_KEY + fault.getRootCauseDeviceId();
        return switch (intent.getIntentType()) {
            case BGP_ADJACENCY         -> 1.0;  // binary: adjacency is down
            case MIN_LINK_AVAILABILITY -> 0.0;  // link availability = 0%
            case MAX_LATENCY -> {
                Object raw = redisTemplate.opsForHash().get(deviceKey, "latencyMs");
                yield raw != null ? Double.parseDouble(raw.toString()) : 0.0;
            }
            case MAX_PACKET_LOSS -> {
                Object raw = redisTemplate.opsForHash().get(deviceKey, "packetLossPercent");
                yield raw != null ? Double.parseDouble(raw.toString()) : 0.0;
            }
        };
    }

    public boolean isFaultEventForIntent(IntentType intentType, String eventType) {
        return switch (intentType) {
            case BGP_ADJACENCY         -> "BGP_SESSION_DOWN".equals(eventType);
            case MIN_LINK_AVAILABILITY -> "LINK_DOWN".equals(eventType);
            case MAX_LATENCY           -> "LATENCY_SPIKE".equals(eventType);
            case MAX_PACKET_LOSS       -> "PACKET_LOSS".equals(eventType);
        };
    }
}