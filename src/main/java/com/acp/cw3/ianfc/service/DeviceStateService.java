
package com.acp.cw3.ianfc.service;

import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.acp.cw3.ianfc.model.enums.DeviceFsmState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.IntentViolation;
import com.acp.cw3.ianfc.repository.CorrelatedIncidentRepository;
import com.acp.cw3.ianfc.repository.IntentViolationRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceStateService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CorrelatedIncidentRepository incidentRepository;
    private final IntentViolationRepository violationRepository;
    private final ViolationService violationService;

    private static final String DEVICE_STATE_KEY = "device:state:";
    private static final String DEVICE_FSM_KEY   = "device:fsm:";

    public void updateState(TelemetryEvent event) {
        String key = DEVICE_STATE_KEY + event.getDeviceId();
        Map<String, String> fields = new HashMap<>();
        fields.put("deviceId",    event.getDeviceId());
        fields.put("region",      event.getRegion() != null ? event.getRegion() : "");
        fields.put("lastUpdated", event.getTimestamp() != null
                ? event.getTimestamp().toString() : Instant.now().toString());

        switch (event.getEventType()) {
            case LINK_DOWN        -> fields.put("linkState", "DOWN");
            case LINK_UP          -> fields.put("linkState", "UP");
            case BGP_SESSION_DOWN -> fields.put("bgpState",  "DOWN");
            case BGP_SESSION_UP   -> fields.put("bgpState",  "ESTABLISHED");
            case LATENCY_SPIKE, LATENCY_NORMAL -> {
                if (event.getMetricValue() != null)
                    fields.put("latencyMs", String.valueOf(event.getMetricValue()));
            }
            case PACKET_LOSS -> {
                if (event.getMetricValue() != null)
                    fields.put("packetLossPercent", String.valueOf(event.getMetricValue()));
            }
        }
        redisTemplate.opsForHash().putAll(key, fields);
        log.debug("Updated device state: key={} fields={}", key, fields.keySet());
    }

    public void updateFsm(TelemetryEvent event) {
        String key = DEVICE_FSM_KEY + event.getDeviceId();
        String raw = redisTemplate.opsForValue().get(key);
        DeviceFsmState current;
        try {
            current = raw != null ? DeviceFsmState.valueOf(raw) : DeviceFsmState.NORMAL;
        } catch (IllegalArgumentException e) {
            current = DeviceFsmState.NORMAL;
        }

        DeviceFsmState next = switch (event.getEventType()) {
            case LINK_DOWN, BGP_SESSION_DOWN -> switch (current) {
                case NORMAL   -> DeviceFsmState.DEGRADED;
                case DEGRADED -> DeviceFsmState.FAILED;
                case FAILED   -> DeviceFsmState.FAILED;
                case RECOVERING -> DeviceFsmState.FAILED;
            };
            case LINK_UP, BGP_SESSION_UP -> switch (current) {
                case FAILED   -> DeviceFsmState.RECOVERING;
                case RECOVERING -> DeviceFsmState.NORMAL;
                case DEGRADED -> DeviceFsmState.NORMAL;
                case NORMAL   -> DeviceFsmState.NORMAL;
            };
            default -> current; // LATENCY_SPIKE, LATENCY_NORMAL, PACKET_LOSS: no FSM change
        };

        if (next != current || raw == null) {
            redisTemplate.opsForValue().set(key, next.name());
            log.debug("FSM transition: deviceId={} {} -> {}", event.getDeviceId(), current, next);
            if (next == DeviceFsmState.NORMAL && current != DeviceFsmState.NORMAL) {
                resolveForDevice(event.getDeviceId());
            }
        }
    }

    public Map<Object, Object> getDeviceState(String deviceId) {
        return redisTemplate.opsForHash().entries(DEVICE_STATE_KEY + deviceId);
    }

    public List<Map<Object, Object>> getAllDeviceStates() {
        Set<String> keys = redisTemplate.keys(DEVICE_STATE_KEY + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        return keys.stream()
                .map(k -> redisTemplate.opsForHash().entries(k))
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toList());
    }

    public String getFsmState(String deviceId) {
        String state = redisTemplate.opsForValue().get(DEVICE_FSM_KEY + deviceId);
        return state != null ? state : "UNKNOWN";
    }

    public Set<String> getAllDeviceIds() {
        Set<String> keys = redisTemplate.keys(DEVICE_STATE_KEY + "*");
        if (keys == null) return Collections.emptySet();
        return keys;
    }

    private void resolveForDevice(String deviceId) {
        Instant now = Instant.now();
        List<CorrelatedFault> active = incidentRepository
                .findByRootCauseDeviceIdAndStatus(deviceId, "ACTIVE");
        for (CorrelatedFault incident : active) {
            incident.setStatus("RESOLVED");
            incident.setResolvedAt(now);
            incidentRepository.save(incident);

            List<IntentViolation> violations = violationRepository
                    .findByFault_FaultIdAndStatus(incident.getFaultId(), "ACTIVE");
            for (IntentViolation v : violations) {
                v.setStatus("RESOLVED");
                v.setResolvedAt(now);
                violationRepository.save(v);
                if (v.getIntent() != null) {
                    violationService.removeActiveViolation(
                            v.getIntent().getIntentId().toString());
                }
            }
            log.info("Resolved incident={} + {} violation(s) for device={}",
                    incident.getFaultId(), violations.size(), deviceId);
        }
    }
}